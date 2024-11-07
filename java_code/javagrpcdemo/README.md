# javagrpcdemo
This folder contains the Apigee Java Callout source code. 

## Process to build an Uber Jar

```shell
mvn clean compile package assembly:single -DskipTests=true
```

## Maven Commands

### Create Jar and include dependencies in build directory
```shell
 mvn clean compile package verify -DskipTests=true
```

### Build Assembly and Save to Apigee Resource Folder.
**Uber jars don't work in Apigee X. Jars must be included indvidually.**
This command will clean the target folder, compile, create the jar file, create the assembly jar with the dependencies and copy the jars to the resources directory.  This is the command that you should run to generate the final jar file with dependencies.

```shell
 mvn clean compile package assembly:single verify -DskipTests=true
```

### Clean, Build, Package the Jar

```shell
mvn clean compile package -DskipTests=true
```

### Execute the tests
```shell
mvn test
```

### Execute Maven Assembly plugin to include main dependencies in master jar
```shell
 mvn assembly:single
```

### Execute Ant-run plugin
Used for troubleshooting and printing the project build directory.
```shell
mvn antrun:run
```

### Install the Jar in the local repo
```shell
mvn install -DskipTests=true
```

## Java Commands
Expand the Java jar file to view the contents.

```shell
jar xf javagrpcdemo-1.8-SNAPSHOT-my-assembly.jar
```

## Upload Java Callout Dependencies
All libs are located in `java_code/java_libs` and they are copied there automatically by the Maven build process. 

```shell
export ORG=ORG
export ENV=ENV
export FILE=FILE

cd apigee_proxies/grpcjavacallout/java_libs

curl -X POST "https://apigee.googleapis.com/v1/organizations/$ORG/environments/$ENV/resourcefiles?name=$FILE&type=java" \
  -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  -H "Content-type:multipart/form-data" \
  -F file=@$FILE
```
### Delete Resources

```shell
export FILE=FILE
curl -X DELETE "https://apigee.googleapis.com/v1/organizations/$ORG/environments/$ENV/resourcefiles/java/$FILE" \
  -H "Authorization: Bearer $(gcloud auth print-access-token)"
  ```

  ## Jar File Troubleshooting
  
  I included these Jar files intiallially, but when I attempted to deploy the proxy I kept getting Class Loading error conflicts. So 
  I removed these three files and then the class loading errors went away. 

  Apparently, these libraries are already included in Apigee. 
  * google-cloud-logging-3.5.1.jar
  * grpc-protobuf-1.53.0.jar
  * protobuf-java-4.28.2.jar

