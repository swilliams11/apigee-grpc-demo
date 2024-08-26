"""The Python implementation of the GRPC helloworld.Greeter client."""

from __future__ import print_function

import logging

import grpc
import external_callout_pb2
import external_callout_pb2_grpc


def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    print("Will try to greet world ...")
    with grpc.insecure_channel("localhost:50051") as channel:
        stub = external_callout_pb2_grpc.ExternalCalloutServiceStub(channel)
        response = stub.ProcessMessage(external_callout_pb2.MessageContext(organization_name="HelloWorld"))
    print("External Callout client received: " + response.organization_name)


if __name__ == "__main__":
    logging.basicConfig()
    run()
