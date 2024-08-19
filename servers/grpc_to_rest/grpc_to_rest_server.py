"""The Python implementation of the gRPC to REST server."""

from concurrent import futures
import logging

import grpc
import grpctorest_pb2
import grpctorest_pb2_grpc
import json


class GrpcToRest(grpctorest_pb2_grpc.GrpcToRestServicer):
    def Convert(self, request, context):
        fname = request.fname
        lname = request.lname
        city = request.city
        http_response_payload = {
            'fname': f'{fname}',
            'lname': f'{lname}',
            'city': f'{city}'
        }
        return grpctorest_pb2.GrpcToRestReply(message=json.dumps(http_response_payload))


def serve():
    port = "50051"
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    grpctorest_pb2_grpc.add_GrpcToRestServicer_to_server(GrpcToRest(), server)
    server.add_insecure_port("[::]:" + port)
    server.start()
    print("Server started, listening on " + port)
    server.wait_for_termination()


if __name__ == "__main__":
    logging.basicConfig()
    serve()
