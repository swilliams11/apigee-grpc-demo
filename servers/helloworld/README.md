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
cd servers
docker build helloworld/ -t grpc-helloworld:v2
docker images -a 
export DOCKER_IMAGE=<imageid>
```

### Run Container locally
```shell
docker run -p 8080:50051 -d $DOCKER_IMAGE
```

### Test the GRPC Server
Execute this from the `servers` directory.

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
1. Create an Artifact repo.
```shell
gcloud artifacts repositories create grpcdemo \
    --repository-format=Docker \
    --location=us \
    --description="gRPC Demo repo"
```


2. Configure Auth Credential Helper for Google Cloud.
```shell
gcloud auth configure-docker us-docker.pkg.dev
```

3. Set environment variables and push the Docker images to Artifactory.
```shell
export REGION=us-west1
export SERVICE_NAME=helloworld-grpc
export PROJECT=PROJECT
docker tag $DOCKER_IMAGE us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME

docker push us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME
```

4. Execute one of the following deployment commands. The first one will configure Cloud Run
to allow unauthenticated clients, and the second command will force all clients to pass a valid 
Authorization header.  

```shell
# Unauthenticated - Allow Cloud Run to accept all requests from all clients
gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --allow-unauthenticated --region $REGION --port 50051

# Authenticated - Clients must include an Authorization header on every request
gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --region $REGION --port 50051
```

5. Test the deployed service.  Execute this command from the `servers` folder. 
**Do not** include the `https://` when you export the `ClOUDRUN_HOSTNAME` environment variable. 

```shell
export CLOUDRUN_HOSTNAME=CR_HOST
grpcurl -import-path ./protos -proto helloworld.proto -d '{"name":"Guest"}' $CLOUDRUN_HOSTNAME:443 helloworld.Greeter/SayHello
```

## Google Cloud Load Balancer (GCLB)
You can either update an existing GCLB to accept gRPC traffic or you can 
create a new Load Balancer. 


### Update Apigee LB to accept gRPC traffic
You can perform the following steps from the gcloud cli (steps shown below) or you can 
complete them in the Google Cloud Console (documentation not included here).

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

#### You must expose the Cloud Run service to the Apigee X Runtime
You also need to complete the following if you haven't done so already.
* Create an Internal Regional Application Load Balancer (IRALB) for the Cloud Run service
* Publish the service with Private Service Connect and use the IRALB as the backend

Request flow is client -> Apigee GLB -> Apigee Runtime -> Endpoint Point Attachment -> IRALB -> Cloud Run


### Create a new Load Balancer (for Apigee) to accept gRPC traffic
High-level outline is provided below. 
1. Create the frontend LB with protocol HTTPS. 
2. Create an SSL certificate
3. Make sure to register the domain name in Cloud DNS and create an A record with the IP address of the LB.
4. For the backend configuration make sure to select HTTP/2 for the protocol.
5. The Host and path rules can be set to the default values and should point to the Apigee Managed Instance Group (MIG).

## Expose Cloud Run via a Service Attachment and Internal Regional Application Load Balancer.
Highlevel outline of steps to expose Cloud Run to the Apigee X Runtime environment. 
1. Create an Internal Regional Application Load Balancer (IRALB)
2. Publish the service in Private Service Connect and use the IRALB as the backend.

## Secure the Apigee Proxy
More detailed steps are provided [here](../../apigee_proxies/helloworld_grpc/README.md).


## Update the gRPC Code
Update the gRPC code if you get warnings or errors in the Cloud Run logs stating that the version of the proto doesn't 
match the gRPC version of the environment.

```shell
python -m grpc_tools.protoc -I../protos --python_out=. --pyi_out=. --grpc_python_out=. ../protos/helloworld.proto

```

Once you execute this command, then you need to build the image, deploy the images to Artifactory and update the Cloud Run service. 