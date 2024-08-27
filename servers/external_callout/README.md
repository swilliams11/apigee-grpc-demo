# Apigee X External Callout policy
This server implements Apigee X's External Callout policy proto in Python.  
View Apigee X's [External Callout proto defined here.](https://github.com/apigee/external-callout/blob/main/server-stubs/java/src/main/proto/external_callout.proto).


## Prerequistes
1. Install [grpcurl](https://github.com/fullstorydev/grpcurl)
2. Install Python
3. Install Docker
4. Install Python Virtual Environment

## Build the gRPC Code
Update the gRPC code if you get warnings or errors in the Cloud Run logs stating that the version of the proto doesn't match the gRPC version of the environment.

Execute this command from the `apigee-grpc-demo` folder.
```shell
python -m grpc_tools.protoc -I ./servers/protos --python_out=./servers/external_callout --pyi_out=./servers/external_callout --grpc_python_out=./servers/external_callout ./servers/protos/external_callout.proto
```

Once you execute this command, then you need to build the image, deploy the images to Artifactory and update the Cloud Run service. 

## External Callout Proto
Review Apigee X's [External Callout proto](https://github.com/apigee/external-callout/blob/main/server-stubs/java/src/main/proto/external_callout.proto) file to see how the MessageContext is formatted.  The [External Callout policy](https://cloud.google.com/apigee/docs/api-platform/reference/policies/external-callout-policy) sends a MessageContext to the gRPC server and expects a MessageContext to be returned from the server.  You can make changes to the headers and flow variables that are passed into the MessageContext and the Example Python gRPC code demonstrates how to do this. 

## Run in Container Locally
### Build the Docker Image
```shell
docker build servers/external_callout/ -t external-callout:v1
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
grpcurl -plaintext -import-path servers/protos -proto servers/protos/external_callout.proto \
-d '{"organization_name":"My Org", "request": {"uri":"/hello", "verb":"POST"}' \
localhost:8080 apigee.ExternalCallout/ProcessMessage
```

## Run via Python locally
Create a virtual environment. 
```shell
python3 -m venv ~/python_venvs/py311
source ~/python_venvs/py311/bin/activate
pip3 install -r ./servers/external_callout/requirements.txt

```

Run the server locally with Python.
```shell
python ./servers/external_callout/external_callout_server.py
```

Run the client locally with Python.
```shell
python ./servers/external_callout/external_callout_client.py
```

Test the server with `grpcurl`.  An example payload is shown below.
```shell
grpcurl -plaintext -import-path servers/protos -proto servers/protos/external_callout.proto \
-d '{"organization_name":"My Org", "request": {"uri":"/hello", "verb":"POST"}, "additional_flow_variables": {"myflow.param": {"string" :" Hellow" }}}' \
localhost:50051 apigee.ExternalCalloutService/ProcessMessage
```

Response:
```shell
External Callout client received: HelloWorld

MessageContext:
organization_name: "HelloWorld"
additional_flow_variables {
  key: "myflow.param"
  value {
    string: "Hellow - MODIFIED in gRPC Server!"
  }
}
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
export SERVICE_NAME=grpcexternalcallout-grpc
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
gcloud run deploy $SERVICE_NAME --image us-docker.pkg.dev/$PROJECT/grpcdemo/$SERVICE_NAME --platform managed --use-http2 --no-allow-unauthenticated --region $REGION --port 50051
```

5. Test the deployed service.  Execute this command from the `servers` folder. 
**Do not** include the `https://` when you export the `ClOUDRUN_HOSTNAME` environment variable. 

```shell
export CLOUDRUN_HOSTNAME=CR_HOST
export AUTH=`gcloud auth print-identity-token`
grpcurl -import-path ./servers/protos -proto external_callout.proto -H "Authorization: Bearer $AUTH" \
-d '{"organization_name":"My Org", "request": {"uri":"/hello", "verb":"POST"}, "additional_flow_variables": {"myflow.param": {"string" :" Hellow" }}}' \
$CLOUDRUN_HOSTNAME:443 apigee.ExternalCalloutService/ProcessMessage
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


