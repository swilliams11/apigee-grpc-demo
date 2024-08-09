FROM python:3.7

WORKDIR /app

COPY . .

RUN pip install grpcio protobuf


EXPOSE 50051

CMD ["python", "greeter_server.py"]
