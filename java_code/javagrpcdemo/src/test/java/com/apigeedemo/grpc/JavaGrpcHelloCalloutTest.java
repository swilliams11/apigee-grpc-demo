package com.apigeedemo.grpc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.apigee.flow.execution.Action;
import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.FlowContext;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;


/**
 * Unit test for simple App.
 */
public class JavaGrpcHelloCalloutTest 
{  
    MessageContext mockedMsgCtxt;
    ExecutionContext mockedExeCtxt;
    Message message;
    FlowContext fc;
    String pattern1 = StringEscapeUtils.unescapeXml("(?i)(&lt;\\s*script\\b[^>]*>[^&lt;]+&lt;\\s*.+\\s*[s][c][r][i][p][t]\\s*>)");
    String grpcTarget = "DOMAIN_NAME";

    @BeforeMethod
    public void setUp() { 
        //Mock the MessageContext
        mockedMsgCtxt = Mockito.mock(MessageContext.class);
        //  {
        //     private Map variables;
        //     public void $init() {
        //         variables = new HashMap();
        //     }

        //     @Mock
        //     public <T> T getVariable(final String name){
        //         if (variables == null) {
        //             variables = new HashMap();
        //         }
        //         return (T) variables.get(name);
        //     }

        //     @Mock()
        //     public boolean setVariable(final String name, final Object value) {
        //         if (variables == null) {
        //             variables = new HashMap();
        //         }
        //         variables.put(name, value);
        //         return true;
        //     }

        //     @Mock()
        //     public boolean removeVariable(final String name) {
        //         if (variables == null) {
        //             variables = new HashMap();
        //         }
        //         if (variables.containsKey(name)) {
        //             variables.remove(name);
        //         }
        //         return true;
        //     }

        // }.getMockInstance();

        // //Mock the ExecutionContext
        mockedExeCtxt = Mockito.mock(ExecutionContext.class);
        message = Mockito.mock(Message.class);
        //when(mockedMsgCtxt.getMessage()).thenReturn(message);
        fc = Mockito.mock(FlowContext.class);
        mockedMsgCtxt.setMessage(fc, message);
    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

     /*
    This method tests that that the Java Callout executes successfully.
    and sends a request to create a customer.
     */
    @Test
    public void testExecute_helloworldGrpcRequest() throws Exception {
        //GIVEN
        Map<String, String> properties = new HashMap<>();
        properties.put("grpc.target", grpcTarget);

        //WHEN
        // this is used to test when the Java Callout gets the grpc.target from the message context instead of the properties map.
        // when(mockedMsgCtxt.getVariable("grpc.target")).thenReturn("YOUR_DOMAIN");
        
        // This code does not work with Mockito. You must mock this getContent() method instead.
        //Message messageLocal = mockedMsgCtxt.getMessage();
        String content = "{\"name\":\"Guest\"}";
        // messageLocal.setContent(content);
        // mockedMsgCtxt.setMessage(fc, messageLocal);

        when(message.getContent()).thenReturn(content);
        when(mockedMsgCtxt.getMessage()).thenReturn(message);

        JavaGrpcHelloCallout callout = new JavaGrpcHelloCallout(properties);
        ExecutionResult result = callout.execute(mockedMsgCtxt, mockedExeCtxt);

        //THEN
        //Assert.assertEquals(result, ExecutionResult.ABORT);
        Assert.assertEquals(result.getAction(), Action.CONTINUE);
        Assert.assertEquals(result.isSuccess(), true);
        // These statements don't work with Mockito; Mockito doesn't create a real object and save data into it.
        // String flowVar = mockedMsgCtxt.getVariable("javaGrpcCallout.end");
        // Message response = mockedMsgCtxt.getResponseMessage();


        }

     /*
    This method tests that that the Java Callout executes successfully.
    and sends a request to create a customer.
     */
    @Test
    public void testExecute_helloworldGrpcRequest_InvalidJSON() throws Exception {
        //GIVEN
        Map<String, String> properties = new HashMap<>();
        properties.put("grpc.target", grpcTarget);

        //WHEN - invalid JSON
        String content = "{\"name\":\"Guest\"";

        when(message.getContent()).thenReturn(content);
        when(mockedMsgCtxt.getMessage()).thenReturn(message);

        JavaGrpcHelloCallout callout = new JavaGrpcHelloCallout(properties);
        ExecutionResult result = callout.execute(mockedMsgCtxt, mockedExeCtxt);

        //THEN - callout should fail
        Assert.assertEquals(result.getAction(), Action.ABORT);
        Assert.assertEquals(result.isSuccess(), false);
        }

    /*
    Helper function to set headers for a test case;
    Single value: .put("x-header", "value")
    Multiple values: .put("x-header", "value1,value2,value3")
     */
    public void setMsgCtxtHeaders(Map<String, String> headers){
        mockedMsgCtxt.setVariable("request.headers.count", headers.size());
        ArrayList<String> headerNames = new ArrayList<>();

        for(Map.Entry<String, String> headerEntry: headers.entrySet()){
            headerNames.add(headerEntry.getKey());
            String [] valuesArray = headerEntry.getValue().split(",");
            List<String> values = Arrays.asList(valuesArray);
            mockedMsgCtxt.setVariable("request.header." + headerEntry.getKey() + ".values", values);
        }
        mockedMsgCtxt.setVariable("request.headers.names", Collections.unmodifiableCollection(headerNames));
    }
}
