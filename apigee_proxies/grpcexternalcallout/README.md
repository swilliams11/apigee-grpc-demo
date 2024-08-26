# grpcExternalCallout Apigee Proxy

This documentation describes how to setup Apigee X to accept gRPC requests.
* Create an Apigee Target Server
* Deploy the Target Server in Apigee
* Deploy the Apigee Proxy
* Deploy the sample developer
* Create an Apigee Product
* Create an Apigee Developer App
* Configure an Endpoint Attachment
* Add a DNS entry in Cloud DNS
* Create a DNS Peering Zone
* Update the Apigee Enviornment Group with the new gRPC host name.
* Test the Apigee Proxy via the Google Cloud Load Balancer

## External Callout Possible Bug
There appears to be an External Callout bug or undocumented function for gRPC requests.

I performed the following:
* I created the gRPC Cloud run server and I can send requests directly to that server via the load balancer and it works.
* I created an Apigee Target Server set as `gRPC - Target` and I included that Target Server in the Apigee Target Endpoint and I send the gRPC request
  via Apigee and it works. Apigee LB -> Cloud Run LB -> Cloud Run.
* I created an Apigee Target Server set as `gRPC - External callout` and I included that Target Server in the Apigee External Callout policy and I receive 
  a SSL error. This is the same Cloud Run instance and network path in my second bullet, which works.
* I created an Apigee Target Server via the CLI and set it as `GRPC` and I included that Target Server in the Apigee External Callout policy and I receive
  a SSL error.  Again, this is the same Cloud Run instance that worked as a `gRPC - Target` (second bullet).


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

apigeecli targetservers import -o $ORG -e $ENV -f apigee_proxies/grpcexternalcallout/config/targetservers.json -t $token

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

3. The Apigee X proxy name will be the name of the zip file, so name the Zip file `grpc-proxy`.
4. Execute the code below to import the proxy and deploy it. 

```shell
export ORG=YOUR_ORG
export ENV=YOUR_ENV
export SA=YOUR_SA
export PROXY_NAME=PROXY_NAME
token=$(gcloud auth print-access-token)

apigeecli apis import -o $ORG -f apigee_proxies/grpcexternalcallout.zip -t $token
apigeecli apis deploy -o $ORG -e $ENV -n $PROXY_NAME --sa $SA -t $token
```

## Deploy the Developer
export DEV=grpc-developer@example.com

```shell
apigeecli developers create -o $ORG -n $DEV -f sample -s developer -u grpc-developer@example.com
```

## Deploy the Apigee Product
```shell
apigeecli products create -o $ORG -e $ENV -m grpctorest-product --grpcopgrp .apigee_proxies/grpcexternalcallout/config/grpc_opsgroup.json -n grpcexternalcallout-product -t $token --approval auto
```


## Deploy the Apigee Developer App
```shell
apigeecli apps create -o $ORG -t $token -e $DEV -n grpcexternalcallout-app -p grpcexternalcallout-product
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
$APIGEE_GRPC_HOST:443 apigee.ExternalCalloutService/ProcessMessage
```