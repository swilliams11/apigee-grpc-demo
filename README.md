# apigee-grpc-demo
The repository demonstrates how to deploy a gRPC enabled Apigee proxy to Apigee X.
* It uses an Apigee Target Server that has the protocol set to gRPC. 
* A helloworld proxy that includes a Verify API step.
* gRPC enabled Apigee product
* A sample developer
* Sample Developer App

## Documentation

1. [Create the gRPC target server in Cloud Run](./servers/README.md).
2. [Create the Apigee Proxy, Target Server, API product, App](./apigee_proxies/helloworld_grpc/README.md)

## TODOS
* Automate deployment of all Apigee configuration items with a shell script
* Automate Cloud Run, Cloud Load Balancer config with Terraform