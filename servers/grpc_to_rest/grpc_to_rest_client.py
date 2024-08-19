"""The Python implementation of the GRPC helloworld.Greeter client."""

from __future__ import print_function

import logging

import grpc
import grpctorest_pb2
import grpctorest_pb2_grpc


def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    print("Will try to greet world ...")
    with grpc.insecure_channel("localhost:50051") as channel:
        stub = grpctorest_pb2_grpc.GrpcToRestStub(channel)
        response = stub.Convert(grpctorest_pb2.GrpcToRestRequest(fname="Peter", lname="Parker", city="New York"))
    print("GrpcToRest client received: " + response.message)


if __name__ == "__main__":
    logging.basicConfig()
    run()
