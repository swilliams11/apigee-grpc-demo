# Helloworld gPRC

Documentation for Java gRPC setup.
https://grpc.io/docs/languages/java/


## Create the gRPC Java files
Execute the following line to create all the necessary gRPC files. However these files already exists in this folder, so there is no need to execute this line. Download [protoc](https://github.com/protocolbuffers/protobuf/releases) for your OS.

From the `servers` directory.  The `protoc` command below will save the output of the protoc command to the `java_codej/javagrpcdemo/src/main/java` directory.

1. download the plugin
2. make it executable
3. execute `protoc`

```shell
curl -LO https://repo1.maven.org/maven2/io/grpc/protoc-gen-grpc-java/1.68.0/protoc-gen-grpc-java-1.68.0-osx-aarch_64.exe

chmod +x protoc-gen-grpc-java-1.68.0-osx-aarch_64.exe

export MYDIR=/Users/USERNAME
export MYPATH=/Users/USERNAME/Documents/GitHub/apigee-grpc-demo/servers
export PROTOPATH=/Users/USERNAME/Documents/GitHub/apigee-grpc-demo/servers
protoc --plugin=protoc-gen-grpc-java=${MYDIR}/protoc-gen-grpc-java-1.68.0-osx-aarch_64.exe --java_out=${MYPATH} --grpc-java_out=${MYPATH} --proto_path=${PROTOPATH}/protos ${PROTOPATH}/protos/helloworld.proto
```

Since the Java Proto files already exists.  All ou have to do is run the following command in the same director as the `pom.xml`.:
4. 
```shell
mvn compile package
```

or5

```shell
mvn compile package -DskipTests=true
```

## Testing
You can test the Java Callout with Visual Studio or running the following Maven command from the same directory as the `pom.xml`.
```shell
mvn test
```

## Troubleshooting Deployment in Apigee

I received this error message when I attempted to deploy this to Apigee.  I need a class file version of 55 or less, which is Java 11. 
```shell
Status: instance "" reported error entities.ConfigurationFailed: "Configuration failure: Caused by: java.lang.UnsupportedClassVersionError: com/apigeedemo/grpc/JavaGrpcHelloCallout has been compiled by a more recent version of the Java Runtime (class file version 59.0), this version of the Java Runtime only recognizes class file versions up to 55.0"