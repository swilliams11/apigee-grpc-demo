package com.apigeedemo.grpc;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.StringPayload;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Severity;



/**
 * Hello world!
 * https://github.com/DinoChiesa/ApigeeEdge-JavaCallout101/blob/master/callout/src/main/java/com/google/apigee/callouts/ExampleCallout.java
 */
public class JavaGrpcHelloCallout implements Execution {
    private static final Pattern variableReferencePattern = Pattern
            .compile("(.*?)\\{([^\\{\\} :][^\\{\\} ]*?)\\}(.*?)");
    private static final String STRING_SETTING_DEFAULT = "Not Set";
    private Map<String, String> properties; // read-only
    private static final Logging logging = LoggingOptions.getDefaultInstance().getService();
    private String logName = "apigee-java-callout";

    public JavaGrpcHelloCallout(Map<String, String> properties) {
        this.properties = properties;
    }

    public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
        // This executes in the IO thread.
        // Any time consumed here will hold the thread for that period.
        Instant stamp = Instant.now();
        String formattedStamp = DateTimeFormatter.ISO_INSTANT.format(stamp);
        Message msg = msgCtxt.getMessage();
        String request = msg.getContent();
        String target = getStringSetting(msgCtxt);
        Optional<HelloReply> helloReply = Optional.empty();

        try {
            helloReply = sendGrpcRequest(msgCtxt, target, request);            
        } catch (InterruptedException e) {
            writeCloudLog(logName, e.getMessage(), Severity.ERROR, null);
            msgCtxt.setVariable("javacallout.error.message", e.getMessage());
            msgCtxt.setVariable("javacallout.exception", e.toString());
        } catch(JSONException e){
            writeCloudLog(logName, e.getMessage(), Severity.ERROR, null);
            msgCtxt.setVariable("javacallout.error.message", e.getMessage());
            msgCtxt.setVariable("javacallout.exception", e.toString());
            return ExecutionResult.ABORT;
        }
        String message = null;
        if(helloReply.isPresent()){
            message = helloReply.get().getMessage();
        }
        

        // set a variable.
        msgCtxt.setVariable("javaGrpcCallout.message", message);
        msg.setHeader("javaGrpcCallout-TimeStamp", formattedStamp);

        Instant end = Instant.now();
        String formattedEnd = DateTimeFormatter.ISO_INSTANT.format(end);
        // set variable and headers
        msgCtxt.setVariable("javaGrpcCallout.end", formattedEnd);
        msg.setHeader("Content-Type", "text/plain");

        // Get a contrived "response payload".
        String result = "status: OK\n"
                + String.format("gRPC response: %s\n", message);

        // Set the content of the current message with that payload.
        // This will be the response.content if the Java callout is
        // configured on the Response flow. It will be the request.content
        // if the policy is configured on the Request flow.
        msg.setContent(result);

        return ExecutionResult.SUCCESS;
    }

    // Function to write to Cloud Logging
    private void writeCloudLog(String logName, String message, Severity severity, Map<String, String> labels) {
        LogEntry.Builder entryBuilder = LogEntry.newBuilder(StringPayload.of(message))
            .setLogName(logName)
            .setSeverity(severity);

        if (labels != null) {
        entryBuilder.setLabels(labels);
        }
        logging.write(Collections.singleton(entryBuilder.build()));
    }

    public Optional<HelloReply> sendGrpcRequest(MessageContext msgCtxt, String target, String request) throws InterruptedException, JSONException {
        JSONObject json = new JSONObject(request);
        String name = json.getString("name");
        HelloRequest helloRequest = HelloRequest.newBuilder().setName(name).build();

        ManagedChannel channel = Grpc.newChannelBuilder(target, TlsChannelCredentials.create()).build();
        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);
        Optional<HelloReply> helloReply = Optional.empty();
        try{
            helloReply = Optional.of(blockingStub.sayHello(helloRequest));
            
        } catch(StatusRuntimeException e) {
            writeCloudLog(logName, "Grpc failure: " + e.getMessage(), Severity.ERROR, null);
            msgCtxt.setVariable("javacallout.exception", e.toString());
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        return helloReply;
    }

    private String getStringSetting(MessageContext msgCtxt) throws IllegalStateException {
        // Retrieve a value from a named property, as a string.
        String value = (String) this.properties.get("grpc.target");
        if (value == null || value.trim().equals("")) {
            return STRING_SETTING_DEFAULT;
        }
        value = resolveVariableReferences(value, msgCtxt);
        if (value == null || value.equals("")) {
            throw new IllegalStateException("value resolves to null or empty.");
        }
        return value;
    }

    /*
     *
     * If a property holds one or more segments wrapped with begin and end
     * curlies, eg, {apiproxy.name}, then "resolve" the value by de-referencing
     * the context variable whose name appears between the curlies.
     **/
    protected String resolveVariableReferences(String spec, MessageContext msgCtxt) {
        if (spec == null || spec.equals(""))
            return spec;
        Matcher matcher = variableReferencePattern.matcher(spec);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "");
            sb.append(matcher.group(1));
            String ref = matcher.group(2);
            String[] parts = ref.split(":", 2);
            Object v = msgCtxt.getVariable(parts[0]);
            if (v != null) {
                sb.append((String) v);
            } else if (parts.length > 1) {
                sb.append(parts[1]);
            }
            sb.append(matcher.group(3));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
