FROM python:3.11

WORKDIR /app

COPY . .

RUN pip install grpcio protobuf grpcio-tools


EXPOSE 50051

CMD ["python", "grpc_to_rest_server.py"]
