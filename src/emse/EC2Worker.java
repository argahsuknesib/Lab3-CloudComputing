package emse;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

public class EC2Worker {
    public static void main(String[] args) throws Exception {
        String firstQueue = "Inbox";
        String secondQueue = "Outbox";

        SqsClient sqsClient = SqsClient.builder().region(Region.US_EAST_1).build();

        String firstQueueURL = createQueue(sqsClient, firstQueue);
        String secondQueueURL = createQueue(sqsClient, secondQueue);

    }

    public static String createQueue(SqsClient sqsClient,String queueName ) {

        try {
            System.out.println("\nCreate Queue");
  
            //create queue request 
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();
  
            sqsClient.createQueue(createQueueRequest);
            
            System.out.println("\nGet queue url");
  
          //get queue url response
            GetQueueUrlResponse getQueueUrlResponse =
                sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
            String queueUrl = getQueueUrlResponse.queueUrl();
            return queueUrl;
  
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
  
    }
}
