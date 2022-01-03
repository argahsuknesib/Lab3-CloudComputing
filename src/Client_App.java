import java.io.*;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import java.util.List;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.FileOutputStream;
import java.io.OutputStream;



public class Client_App {
    public static void main(String[] args) {
        
        //initializing bucket name
        String bucketName = "bucket.emse.cloud.project.final";
        
        //initializing s3 client &  region
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder()
                              .region(region)
                              .build();

        //initializing key
        String objectKey = "sales-2021-01-02.csv";
        
        //initializing file path
        String objectPath = "sales-2021-01-02.csv";


        createBucket (s3, bucketName);

    
        String result = putS3Object(s3, bucketName, objectKey, objectPath);
        System.out.println("Tag information: "+result);
        s3.close();


        //initializing queue name
        String queueName1 = "Inbox";

        //initializing message content
        String message =  bucketName + "," + objectKey;
        
        //initializing sqs client & region
        SqsClient sqsClient = SqsClient.builder()
            .region(Region.US_EAST_1)
            .build();
            sendMessage(sqsClient, queueName1, message);
            sqsClient.close();

            System.out.println("Waiting few seconds to receive the messege from Outbox queue");
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        //retrieve message from Outbox queue
        SqsClient sqsClient2 = SqsClient.builder()
        .region(Region.US_EAST_1)
        .build();
        //Outbox queue url
        String Url2="https://sqs.us-east-1.amazonaws.com/004250956885/Outbox";
        String message_content = RetrieveMessage(sqsClient2, Url2);
        System.out.println(message_content);

        //System.out.println(message_content.length());
        String[] output = message_content.split("[,]", 0);
        String bucketName2 = output[0]; // bucket.emse.cloud.project.final
        String keyName2 = output[1]; // sales-2021-01-02.csv
        System.out.println("Message retrieved from bucket: "+ bucketName2);
        System.out.println("File name: " + keyName2);
        sqsClient.close();

        //retrieve file
        String path2 = "C:\\Users\\adminlocal\\Desktop\\Lab3-CloudComputing\\result2.csv";
        //initializing s3 client &  region
        //initializing s3 client &  region
        S3Client s3_2 = S3Client.builder()
            .region(region)
            .build();   
        
        getObjectBytes(s3_2,bucketName,keyName2, path2);
    }   

    public static void createBucket( S3Client s3Client, String bucketName) {

        try {
            S3Waiter s3Waiter = s3Client.waiter();
            //create bucket request
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            //head bucket request
            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            // wait until the bucket is created and print out the response.
            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println(bucketName +" has been created\n");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    
    public static String putS3Object(S3Client s3,
                                     String bucketName,
                                     String objectKey,
                                     String objectPath) {
                                         
        try {

            Map<String, String> metadata = new HashMap<>();
            metadata.put("x-amz-meta-myVal", "test");
            
            // put object request
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .metadata(metadata)
                    .build();
            
            // put object response
            PutObjectResponse response = s3.putObject(putOb,
                    RequestBody.fromBytes(getObjectFile(objectPath)));
            System.out.println("File " + objectKey +" uploaded into bucket "+bucketName);
            System.out.println();

           return response.eTag();

        } catch (S3Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
             
        }
        return "";
    }
    
    // Return a byte array
    private static byte[] getObjectFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return bytesArray;
    }

    public static void sendMessage(SqsClient sqsClient, String queueName, String message) {

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();
            CreateQueueResponse createResult = sqsClient.createQueue(request);
  
            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
            
            //get queue url
            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
            
            // send message request
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .delaySeconds(5)
                .build();
  
            sqsClient.sendMessage(sendMsgRequest);
            System.out.println("\nClient Message is sent to the Inbox queue");
  
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static String RetrieveMessage(SqsClient sqsClient, String Url) {
	        
        try {
            // Receive messages from the queue
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(Url)
                .build();

            List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();
            String message_content = "";

            // Print out the messages
            //System.out.println("\nMassege content: ");
             for (Message m : messages) {
                //System.out.println("\n" +m.body());
                 message_content = m.body();
             }

             
            deleteMessages(sqsClient, Url,  messages);
            return(message_content);

        } catch (QueueNameExistsException e) {
            throw e;
        }
        
    }
    
    public static void deleteMessages(SqsClient sqsClient, String queueUrl,  List<Message> messages) {

  
        try {
            for (Message message : messages) {
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
                sqsClient.deleteMessage(deleteMessageRequest);
                System.out.println("\nMessage is deleted.");
            }
  
  
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
   }

   public static void getObjectBytes (S3Client s3, String bucketName, String keyName, String path ) {
	
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();

            // Write the data to a local file
            File myFile = new File(path );
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            System.out.println("Successfully obtained file from an S3 object");
            os.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
        System.err.println(e.awsErrorDetails().errorMessage());
        System.exit(1);
        }
    }

}
