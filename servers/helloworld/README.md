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
docker build helloworld/ -t grpc-helloworld:v1
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
export REGION=us-central1
export SERVICE_NAME=helloworld-grpc
export PROJECT=PROJECT
docker tag $DOCKER_IMAGE us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME

docker push us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME

gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --allow-unauthenticated --region $REGION --port 50051

```

Test the deployed service.
```shell
grpcurl -import-path ./protos -proto helloworld.proto -d '{"name":"Guest"}' helloworld-grpc-l3jikl46na-uc.a.run.app:443 helloworld.Greeter/SayHello
```

## Update Apigee LB to accept GRPC traffic

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
slCertificates:
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

10. 