"""The Python implementation of the gRPC to REST server."""

from concurrent import futures
import logging

import grpc
import external_callout_pb2
import external_callout_pb2_grpc
import json
from typing import Dict


class ExternalCalloutService(external_callout_pb2_grpc.ExternalCalloutServiceServicer):
    def ProcessMessage(self, request, context):
        messageContext = request
        # generates an error: ValueError: Message objects may not be assigned
        # messageContext.additional_flow_variables["myflow.param"] = external_callout_pb2.FlowVariable(string="Hello World!")
        print_flow_variables(messageContext.additional_flow_variables)

        # Demo how to edit an existing flow variable.
        edit_first_flow_var(messageContext)

        # TODO Demo how to add a flow variable.

        return messageContext
        #return external_callout_pb2.MessageContext(request=external_callout_pb2.Request(verb="POST"))

def edit_first_flow_var(messageContext: external_callout_pb2.MessageContext):
    """
    Edit the first flow variable as long as it is a string type.

    Keyword arguments:
    messageContext - the message context of the request.
    """
    flow_variables = messageContext.additional_flow_variables
    vars_len = len(flow_variables)
    print(vars_len)
    if vars_len > 0:
        for key in flow_variables:
            # print(flow_variables[key].WhichOneof("value"))
            # print(flow_variables[key].WhichOneof("value") == "string")
            if flow_variables[key].WhichOneof("value") == "string":
                updated_value = flow_variable_helper_value(flow_variables[key]) + " - MODIFIED in gRPC Server!"
                print(f"flow var name: {key}, value_old: {flow_variable_helper_value(flow_variables[key])}, value_new: {updated_value}")
                messageContext.additional_flow_variables[key].string = updated_value
            break

            
def print_flow_variables(flow_variables: Dict[str, external_callout_pb2.FlowVariable]):
    """
    Prints all the flow variables.  

    Keyword arguments:
    flow_variables: Dict[str, external_callout_pb2.FlowVariable] - dictionary of all the flow variables in the request
    """
    for key in flow_variables:
        print(f"flow variable name: {key} value: {flow_variable_helper_value(flow_variables[key])}")


def flow_variable_helper_value(flow_var: external_callout_pb2.FlowVariable):
    """
    Returns the FlowVariable value from the correct field, given the type.
    """
    flow_var_type = flow_var.WhichOneof("value")
    match flow_var_type:
        case "string":
            return flow_var.string
        case "int32":
            return flow_var.int32
        case "int64":
            return flow_var.int64
        case "bool":
            return flow_var.bool
        case "double":
            return flow_var.double


def serve():
    port = "50051"
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    external_callout_pb2_grpc.add_ExternalCalloutServiceServicer_to_server(ExternalCalloutService(), server)
    server.add_insecure_port("[::]:" + port)
    server.start()
    print("Server started, listening on " + port)
    server.wait_for_termination()


if __name__ == "__main__":
    logging.basicConfig()
    serve()
