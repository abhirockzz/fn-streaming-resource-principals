package com.example.fn;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.streaming.StreamClient;
import com.oracle.bmc.streaming.model.PutMessagesDetails;
import com.oracle.bmc.streaming.model.PutMessagesDetailsEntry;
import com.oracle.bmc.streaming.model.PutMessagesResult;
import com.oracle.bmc.streaming.model.PutMessagesResultEntry;
import com.oracle.bmc.streaming.model.StreamSummary;
import com.oracle.bmc.streaming.requests.ListStreamsRequest;
import com.oracle.bmc.streaming.requests.PutMessagesRequest;
import com.oracle.bmc.streaming.responses.ListStreamsResponse;

import java.util.Arrays;
import java.util.List;

public class StreamProducerFunction {

    private StreamAdminClient sAdminClient = null;
    private StreamClient streamClient = null;
    final ResourcePrincipalAuthenticationDetailsProvider provider
            = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

    public StreamProducerFunction() {

        //print env vars in Functions container
        System.err.println("OCI_RESOURCE_PRINCIPAL_VERSION " + System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION"));
        System.err.println("OCI_RESOURCE_PRINCIPAL_REGION " + System.getenv("OCI_RESOURCE_PRINCIPAL_REGION"));
        System.err.println("OCI_RESOURCE_PRINCIPAL_RPST " + System.getenv("OCI_RESOURCE_PRINCIPAL_RPST"));
        System.err.println("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM " + System.getenv("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM"));

        try {
           
            sAdminClient = new StreamAdminClient(provider);
            String region = System.getenv().get("STREAM_REGION"); //e.g. us-phoenix-1
            sAdminClient.setEndpoint("https://streams." + region + ".streaming.oci.oraclecloud.com");

        } catch (Throwable ex) {
            System.err.println("Error occurred in StreamProducerFunction constructor - " + ex.getMessage());
        }
    }

    public String produce(Message msg) {
        String result = null;

        if (sAdminClient == null) {
            result = "Stream Admin client is not ready. Please check for errors in constructor";
            System.out.println(result);
            return result;
        }

        try {

            ListStreamsRequest listStreamsRequest
                    = ListStreamsRequest.builder()
                            .name(msg.streamName)
                            .compartmentId(msg.compartmentOCID)
                            .build();

            System.out.println("listing streams in compartment "+ msg.compartmentOCID);
            ListStreamsResponse listStreamsResponse = sAdminClient.listStreams(listStreamsRequest);
            List<StreamSummary> streams = listStreamsResponse.getItems();

            if (streams.isEmpty()) {
                String errMsg = "Stream with name " + msg.streamName + " not found in compartment " + msg.compartmentOCID;
                System.out.println(errMsg);
                return errMsg;
            }

            String streamOCID = streams.get(0).getId();
            System.out.println("Found stream with OCID -- " + streamOCID);

            String streamClientEndpoint = streams.get(0).getMessagesEndpoint();
            System.out.println("Stream client endpoint " + streamClientEndpoint);

            streamClient = new StreamClient(provider);
            streamClient.setEndpoint(streamClientEndpoint);

            PutMessagesDetails putMessagesDetails
                    = PutMessagesDetails.builder()
                            .messages(Arrays.asList(PutMessagesDetailsEntry.builder().key(msg.key.getBytes()).value(msg.value.getBytes()).build()))
                            .build();

            PutMessagesRequest putMessagesRequest
                    = PutMessagesRequest.builder()
                            .putMessagesDetails(putMessagesDetails)
                            .streamId(streamOCID)
                            .build();

            PutMessagesResult putMessagesResult = streamClient.putMessages(putMessagesRequest).getPutMessagesResult();
            System.out.println("pushed messages...");

            for (PutMessagesResultEntry entry : putMessagesResult.getEntries()) {
                if (entry.getError() != null) {
                    result = "Put message error " + entry.getErrorMessage();
                    System.out.println(result);
                } else {
                    result = "Message pushed to offset " + entry.getOffset() + " in partition " + entry.getPartition();
                    System.out.println(result);
                }
            }
        } catch (Exception e) {
            result = "Error occurred - " + e.getMessage();

            System.out.println(result);
        }
        /*finally {
            sAdminClient.close();
            streamClient.close();
            System.out.println("Closed stream clients");
        }*/

        return result;
    }

    public static class Message {

        private String streamName;
        private String compartmentOCID;
        private String key;
        private String value;

        public String getStreamName() {
            return streamName;
        }

        public void setStreamName(String streamName) {
            this.streamName = streamName;
        }

        public String getCompartmentOCID() {
            return compartmentOCID;
        }

        public void setCompartmentOCID(String compartmentOCID) {
            this.compartmentOCID = compartmentOCID;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

}
