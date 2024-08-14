# helloworld_grpc Apigee Proxy


## Prerequisites
Install [apigeecli](https://github.com/apigee/apigeecli/tree/main)

## Deploy the Target Server
This will create a target server in the Apigee Environment with TLS enabled.  
You should register the DNS in Cloud DNS and add an A Record with the IP address of
the PSC Endpoint Attachment, which will point to the Internal Regional Load Balancer for Cloud Run.  

```shell
cd helloworld_grpc
export ORG=YOUR_ORG
export ENV=YOUR_ENV
token=$(gcloud auth print-access-token)

apigeecli targetservers import -o $ORG -e $ENV -f config/targetservers.json -t $token

```

## Deploy the Apigee Proxy
This code will deploy the Apigee proxy. 

1. You have to create an Service Account that has the Cloud Run Invoker role (use the Cloud Console to complete this).
2. Zip the `apiproxy` folder after you update the Target Server's Authentication Audience.
3. The Apigee X proxy name will be the name of the zip file.
4. Execute the code below to import the proxy and deploy it. 

```shell
cd helloworld_grpc
export ORG=YOUR_ORG
export ENV=YOUR_ENV
export SA=YOUR_SA
export PROXY_NAME=PROXY_NAME
token=$(gcloud auth print-access-token)

apigeecli apis import -o $ORG -f helloworld_gprc_delete.zip -t $token
apigeecli apis deploy -o $ORG -e $ENV -n $PROXY_NAME --sa $SA -t $token
```

## Deploy the Developer
export DEV=grpc-developer@example.com

```shell
apigeecli developers create -o $ORG -n $DEV -f sample -s developer -u grpc-developer@example.com
```

## Deploy the Apigee Product
```shell
apigeecli products create -o $ORG -e $ENV -m helloworld-grpc --grpcopgrp ./config/grpc_opsgroup.json -n helloworld-grpc -t $token --approval auto
```


## Deploy the Apigee Developer App
```shell
apigeecli apps create -o $ORG -t $token -e $DEV -n grpc-app-test -p helloworld-grpc
```

## Test the proxy
```shell
cd ../..

export APIGEE_GRPC_HOST=YOUR_PROXY
export APIKEY=YOURKEY
grpcurl -import-path ../../servers/protos -proto helloworld.proto -H "apikey: $APIKEY" -d '{"name":"Guest"}' $APIGEE_GRPC_HOST:443 helloworld.Greeter/SayHello
```