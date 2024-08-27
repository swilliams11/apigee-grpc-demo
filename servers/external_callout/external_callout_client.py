"""The Python implementation of the GRPC helloworld.Greeter client."""

from __future__ import print_function

import logging

import grpc 
from external_callout_pb2 import FlowVariable, MessageContext
import external_callout_pb2_grpc


def run():
    # NOTE(gRPC Python Team): .close() is possible on a channel and should be
    # used in circumstances in which the with statement does not fit the needs
    # of the code.
    print("Will try to greet world ...")
    with grpc.insecure_channel("localhost:50051") as channel:
        stub = external_callout_pb2_grpc.ExternalCalloutServiceStub(channel)
        hellow_flow_var = FlowVariable(string="Hellow")
        response: MessageContext = stub.ProcessMessage(MessageContext(organization_name="HelloWorld", additional_flow_variables={"myflow.param": hellow_flow_var}))
    print("External Callout client received: " + response.organization_name)

    print(f"\nMessageContext:\n{response}")


if __name__ == "__main__":
    logging.basicConfig()
    run()
