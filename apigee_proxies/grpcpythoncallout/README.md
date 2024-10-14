# grpcPython Callout Apigee Proxy

This repository uses a Python callout to call a gRPC service.

Unfortunately, Python runs in Apigee using [Jython 2.5.2](https://cloud.google.com/apigee/docs/api-platform/reference/policies/python-script-policy) and this is too old to run my gRPC python code. 
* [Jython 2.5 Github](https://github.com/jython/jython/tree/2.5)

**Instead of rewriting the code to get gRPC to work with Python 2.5, I've decided to use a JavaCallout instead. Therefore, this folder/code is incomplete and does not work.**
 
## Prerequisites
* Install [apigeecli](https://github.com/apigee/apigeecli/tree/main)
* Install [grpcurl](https://github.com/fullstorydev/grpcurl)

## Deploy the Target Server
This will create a target server in the Apigee Environment with TLS enabled.  
You should register the DNS in Cloud DNS and add an A Record with the IP address of
the PSC Endpoint Attachment, which will point to the Internal Regional Load Balancer for Cloud Run.  

```shell
export ORG=YOUR_ORG
export ENV=YOUR_ENV
token=$(gcloud auth print-access-token)

apigeecli targetservers import -o $ORG -e $ENV -f apigee_proxies/grpcpythoncallout/config/targetservers.json -t $token

```

## Deploy the Apigee Proxy
This code will deploy the Apigee proxy. 

1. You have to create an Service Account that has the Cloud Run Invoker role (use the Cloud Console to complete this).
2. Zip the `apiproxy` folder **after** you update the Target Server's Authentication Audience (see below).
   ```xml
    <Authentication>
      <GoogleIDToken>
        <Audience>https://YOURCLOUDRUNDOMAIN</Audience>
      </GoogleIDToken>
    </Authentication>
    ```

3. The Apigee X proxy name will be the name of the zip file, so name the Zip file `grpcpythoncallout`.
4. Execute the code below to import the proxy and deploy it. 

```shell
export ORG=YOUR_ORG
export ENV=YOUR_ENV
export SA=YOUR_SA
export PROXY_NAME=grpcpythoncallout
token=$(gcloud auth print-access-token)

cd apigee_proxies/grpcpythoncallout
zip -r grpcpythoncallout.zip apiproxy
cd ../..
apigeecli apis import -o $ORG -f apigee_proxies/grpcpythoncallout -t $token
apigeecli apis deploy -o $ORG -e $ENV -n $PROXY_NAME --sa $SA -t $token
```

## Deploy the Developer
export DEV=grpc-developer@example.com

```shell
apigeecli developers create -o $ORG -n $DEV -f sample -s developer -u grpc-developer@example.com
```

## Deploy the Apigee Product
```shell
apigeecli products create -o $ORG -e $ENV -m grpcpythoncallout-product --grpcopgrp .apigee_proxies/grpcpythoncallout/config/grpc_opsgroup.json -n grpcpythoncallout-product -t $token --approval auto
```


## Deploy the Apigee Developer App
```shell
apigeecli apps create -o $ORG -t $token --email $DEV -n grpcpythoncallout-app -p grpcpythoncallout-product
```

## Configure an Endpoint Attachment in Apigee
You must configure an Endpoint Attachment in Apigee that points to the Internal Regional Application Load Balancer for the Cloud Run service. 

## Create a DNS entry in Cloud Run
* You can create a DNS entry in Cloud DNS and give it the private IP address of the Service Endpoint Attachment
* You must also create DNS peering zone with the Google Cloud CLI so that Apigee X Runtime can resolve the DNS name.  

## Update Apigee Environment Group
Update the Apigee Environment Group to include the new gRPC domain. 


## Test the proxy
```shell
export APIGEE_GRPC_HOST=YOUR_PROXY
export APIKEY=YOURKEY

grpcurl -import-path ./servers/protos -proto external_callout.proto \
-H "apikey: $APIKEY" \
-d '{"organization_name":"My Org", "request": {"uri":"/hello", "verb":"POST"}, "additional_flow_variables": {"myflow.param": {"string" :" Hellow" }}}' \
$APIGEE_GRPC_HOST:443 apigee.PythonCalloutService/ProcessMessage
```