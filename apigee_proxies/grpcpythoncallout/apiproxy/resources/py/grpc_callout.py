# import base64
import grpc
import json

# Import the gRPC generated proto files
import helloworld_pb2
import helloworld_pb2_grpc

request_payload = flow.getVariable("request.content")
request_domain = flow.getVariable("grpcDomain")
request_port = flow.getVariable("grpcPort")
data = json.loads(request_payload)
guest_name = data['name']

# send the request to gRPC server
print("Will try to greet world ...")
with grpc.secure_channel(f"{request_domain}:{request_port}") as channel:
    stub = helloworld_pb2_grpc.GreeterStub(channel)
    response = stub.SayHello(helloworld_pb2.HelloRequest(name=f"{guest_name}"))
print("gRPC client received: " + response.message)

flow.setVariable("grpcResponsePayload", response.message)
