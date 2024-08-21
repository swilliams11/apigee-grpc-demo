# gRPC to REST
This server converts a gRPC payload to REST by saving the entire gRPC payload as a single JSON
property in the gRPC response. I originally created it so that it could be called by an Apigee X External Callout policy.  However, the External Callout policy requires that you implement a specific proto.  

The only way this gRPC can be called from Apigee X is as a Target Endpoint.  

## Prerequistes
1. Install [grpcurl](https://github.com/fullstorydev/grpcurl)
2. Install Python
3. Install Docker
4. Install Python Virtual Environment


## Run in Container Locally
### Build the Docker Image
```shell
cd servers
docker build grpc_to_rest/ -t grpc-to-rest:v1
docker images -a 
export DOCKER_IMAGE=<imageid>
```

### Run Container locally
```shell
docker run -p 8080:50051 -d $DOCKER_IMAGE
```

### Test the gRPC Server
Execute this from the `servers` directory.

```shell
cd servers

grpcurl -plaintext -import-path protos -proto grpctorest.proto -d '{"fname":"Guest", "lname": "Parker", "city":"New York"}' localhost:8080 grpctorest.GrpcToRest/Convert
```

## Run via Python locally
Create a virtual environment. 
```shell
cd servers/grpc_to_rest
python3 -m venv ~/python_venvs/py311
source ~/python_venvs/py311/bin/activate
pip3 install -r requirements.txt

```

Run the server locally.
```shell
python grpctorest_server.py
```

Run the client locally.
```shell
python grpctorest_client.py
```


## Upload Container to Google Cloud Artifactory
1. Create an Artifact repo. If already completed this step then you don't need to execute it again. 
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
export SERVICE_NAME=grpctorest-grpc
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
export AUTH=`gcloud auth print-identity-token`
grpcurl -import-path ./protos -proto grpctorest.proto -H "Authorization: Bearer $AUTH" -d '{"fname":"Guest","lname":"World","city": "New York"}' $CLOUDRUN_HOSTNAME:443 grpctorest.GrpcToRest/Convert
```

## Google Cloud Load Balancer (GCLB)
Review docs [here](../helloworld/README.md#google-cloud-load-balancer-gclb)

### Update Apigee LB to accept gRPC traffic
Review docs [here](../helloworld/README.md#update-apigee-lb-to-accept-grpc-traffic)
 

#### You must expose the Cloud Run service to the Apigee X Runtime
Review docs [here](../helloworld/README.md#you-must-expose-the-cloud-run-service-to-the-apigee-x-runtime)


### Create a new Load Balancer (for Apigee) to accept gRPC traffic
Review docs [here](../helloworld/README.md#create-a-new-load-balancer-for-apigee-to-accept-grpc-traffic)


## Expose Cloud Run via a Service Attachment and Internal Regional Application Load Balancer.
Review docs [here](../helloworld/README.md#expose-cloud-run-via-a-service-attachment-and-internal-regional-application-load-balancer)


## Secure the Apigee Proxy
More detailed steps are provided [here](../../apigee_proxies/helloworld_grpc/README.md).


## Update the gRPC Code
Update the gRPC code if you get warnings or errors in the Cloud Run logs stating that the version of the proto doesn't 
match the gRPC version of the environment.

```shell
python -m grpc_tools.protoc -I../protos --python_out=. --pyi_out=. --grpc_python_out=. ../protos/grpctorest.proto

```

Once you execute this command, then you need to build the image, deploy the images to Artifactory and update the Cloud Run service. 