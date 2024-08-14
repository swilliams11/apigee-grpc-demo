[This code's documentation lives on the grpc.io site.](https://grpc.io/docs/languages/python/quickstart)


# GRPC Helloworld sample from GRPC demo site.
## Prerequistes
1. Install [grpcurl](https://github.com/fullstorydev/grpcurl)
2. Install Python
3. Install Docker
4. Install Python Virtual Environment


## Run in Container Locally
### Build the Docker Image
```shell
docker build helloworld/ -t grpc-helloworld:v2
docker images -a 
export DOCKER_IMAGE=<imageid>
```

### Run Container locally
```shell
docker run -p 8080:50051 -d $DOCKER_IMAGE
```

### Test the GRPC Server
```shell
cd servers
grpcurl -plaintext -import-path ../protos -proto helloworld.proto -d '{"name":"Guest"}' localhost:8080 helloworld.Greeter/SayHello
```

## Run via Python locally
Create a virtual environment. 
```shell
cd servers/helloworld
python3 -m venv ~/python_venvs/py311
source ~/python_venvs/py311/bin/activate
pip3 install -r requirements.txt

```

Run the server locally.
```shell
python greeter_server.py
```

Run the client locally.
```shell
python greeter_client.py
```


## Upload Container to Google Cloud Artifactory
Create an Artifact repo.
```shell
gcloud artifacts repositories create grpcdemo \
    --repository-format=Docker \
    --location=us \
    --description="gRPC Demo repo"
```


Configure Auth Credential Helper for Google Cloud.
```shell
gcloud auth configure-docker us-docker.pkg.dev
```

```shell
export REGION=us-west1
export SERVICE_NAME=helloworld-grpc
export PROJECT=PROJECT
docker tag $DOCKER_IMAGE us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME

docker push us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME

# Unauthenticated
gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --allow-unauthenticated --region $REGION --port 50051

# Authenticated
gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --region $REGION --port 50051
```

Test the deployed service.
```shell
grpcurl -import-path ./protos -proto helloworld.proto -d '{"name":"Guest"}' YOUR_HOSTNAME:443 helloworld.Greeter/SayHello
```

## Google Cloud Load Balancer
You can either update an existing GCLB to accept gRPC traffic or you can 
create a new Load Balancer. 


### Update Apigee LB to accept GRPC traffic
You can perform the following steps from the gcloud cli or you can 
complete them in the Google Cloud Console.

1. Get the forwarding rules in your project. 
```shell
gcloud compute forwarding-rules list
```

2. Create the forwarding rules environment variables using the Apigee LB forwarding rules.
```shell
export FORWARDING_RULE=<NAME from above>
export IP_ADDRESS=<IP_ADDRESS from above>
export TARGET_PROXY=<TARGET from above>
```

3. Get the URL Map and SSL Cert.
```shell
gcloud compute target-https-proxies describe $TARGET_PROXY
```

Shell Output
```shell
sslCertificates:
- https://www.googleapis.com/compute/v1/projects/PROJECT/global/sslCertificates/apigee-ssl-cert
tlsEarlyData: DISABLED
urlMap: https://www.googleapis.com/compute/v1/projects/PROJECT/global/urlMaps/apigee-proxy-url-map
```

4. Create environment variables. 
In this example the `URL_MAP` would be `apigee-proxy-url-map` and 
the `SSL_CERT` would be `apigee-ssl-cert`

```shell
export URL_MAP=<url map name from above
export SSL_CERT=<certificate name from above>
```

5. Get the SSL Cert domains.
```shell
gcloud compute ssl-certificates describe $SSL_CERT  --format json | jq .managed.domains
```

6. Create a new certificate. 
```shell
export DOMAINS="<existing domain name>","grpc.$IP_ADDRESS.nip.io"
gcloud compute ssl-certificates create apigee-ssl-grpc \
       --domains $DOMAINS

```

7. Update the target proxy to use the new cert. 
```shell
gcloud compute target-https-proxies update $TARGET_PROXY --ssl-certificates apigee-ssl-grpc
```

8. Create a backend service that supports HTTP2 (GRPC).
```shell
gcloud compute backend-services create apigee-grpc \
  --load-balancing-scheme=EXTERNAL_MANAGED \
  --protocol=HTTP2 \
  --global --project=$PROJECT
```

9. Get the name of the Apigee NEG.
```shell
gcloud compute network-endpoint-groups list
```

10. Update the backend service to the Apigee NEG.
TODO add alternative to add VM instance group Backend as well. 

```shell
export APIGEE_NEG=<NAME from above>
gcloud compute backend-services add-backend apigee-grpc \
  --network-endpoint-group=$APIGEE_NEG \
  --network-endpoint-group-region=<LOCATION from above> \
  --global --project=$PROJECT
```

11. Get the URL Map and edit it.
```shell
gcloud compute url-maps edit $URL_MAP
```

```yaml
hostRules:
- hosts:
  - YOUR_DOMAIN_NAME
  pathMatcher: grpc-domain
name: apigee-lb
pathMatchers:
- defaultService: https://www.googleapis.com/compute/v1/projects/<PROJECT_ID>/global/backendServices/apigee-grpc
  name: grpc-domain
```

12. Update the Apigee Environment Group to include the new grpc domain. 

13. Create a Target Server in Apigee that supports the GRPC protocol and use the hostname from the Cloud Run server you deployed earlier.
    * Target server name: `grpc-hello`
    * gRPC - Target
    * Port: 443
    * Enable SSL: True

14. Create an Apigee proxy named `grpc-proxy`.
    You can include any domain name as the Target Server because you will override it with the code below.

    ```xml
    <HTTPTargetConnection>
    <LoadBalancer>
      <Server name="grpc-hello"/>
    </LoadBalancer>
    <Path>/helloworld.Greeter/SayHello</Path>
  </HTTPTargetConnection>

    ```

    ```xml
    <Authentication>
      <GoogleIDToken>
        <Audience>https://YOURCLOUDRUNDOMAIN</Audience>
      </GoogleIDToken>
    </Authentication>
    ```

15. When you deploy the proxy, you have to deploy it with a Service Account that has the Cloud Run Invoker role. 

16. You also need to complete the following if you haven't done so already.
    * Create an Internal Regional Application Load Balancer for the Cloud Run service
    * Publish the service with PSC and use the ILB as the backend
    * Create a Service Endpoint Attachment in Apigee and refer to the service you just published
    * You can create a DNS entry in Cloud DNS and give it the private IP address  of the Service Endpint Attachment.
    * Flow is Apigee Runtime -> Endpoint Point Attachment -> ILB - Cloud Run

17. Test the Apigee proxy.  This assumes that you have disabled the Verify API Policy in the Preflow.

```shell
export APIGEE_GRPC_HOST=YOUR_PROXY
grpcurl -import-path ../protos -proto helloworld.proto -d '{"name":"Guest"}' $APIGEE_GRPC_HOST:443 helloworld.Greeter/SayHello
```

### Create a new LB to accept GRPC traffic
1. Create the frontend LB with protocol HTTPS. 
2. Create an SSL certificate
3. Make sure to register the domain name in Cloud DNS and create an A record with the IP address of the LB.
4. For the backend configuration make sure to select HTTP/2 for the protocol.
5. The Host and path rules can be set to the default values.


## Secure the Apigee Proxy
1. The proxy has the Verify API Key Policy requesting that the API Key is passed into the `apikey` header.

2. Create an Apigee Product and Developer App.
    * Service Name: `helloworld.Greeter`
    * Method: `SayHello`


3. Test the proxy.
```shell
export APIGEE_GRPC_HOST=YOUR_PROXY
export APIKEY=YOURKEY
grpcurl -import-path ../protos -proto helloworld.proto -H "apikey: $APIKEY" -d '{"name":"Guest"}' $APIGEE_GRPC_HOST:443 helloworld.Greeter/SayHello
```

## Update the GRPC Code
Update the GRPC code if you get warnings or errors in the Cloud Run logs stating that the version of the proto doesn't 
match the GRPC version of the environment.

```shell
python -m grpc_tools.protoc -I../protos --python_out=. --pyi_out=. --grpc_python_out=. ../protos/helloworld.proto

```