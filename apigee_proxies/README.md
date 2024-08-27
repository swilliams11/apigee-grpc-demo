# Apigee Proxies

## Summary
This folder contains the following Apigee Proxies to be used in conjunction with the gRPC servers.

## Apigee Proxies
1. [gRPC External Callout](./grpcexternalcallout/README.md)
    This proxy uses Apigee X's External Callout policy; however, there appears to be a **bug** in the Target Server definition so the External Callout policy doesn't work.  I've submitted a ticket to our Engineering team to review this error.  
2. [gRPC to REST](./grpctorest/README.md)
    This was my original gRPC to REST test proxy, but I decided not to continue testing it; therefore, the proxy code is not saved here.
3. [helloworld gRPC](./helloworld_grpc/README.md)
    This proxy sends a gRPC request via a Target Endpoint (not an External Callout policy) and it works successfully. 