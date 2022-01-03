import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import java.util.List;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;  
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class EC2_Worker{

  public static void main(String[] args) {
	  
	  //initializing queue name
      String queueName1 = "Inbox";
      String queueName2 = "Outbox";
      
      //queue url
      String Url="https://sqs.us-east-1.amazonaws.com/004250956885/Inbox";

      //initializing sqs client & region
      SqsClient sqsClient = SqsClient.builder()
              .region(Region.US_EAST_1)
              .build();
      
      //String queueUrl1 = createQueue(sqsClient, queueName1 );
      //String queueUrl2 = createQueue(sqsClient, queueName2 );

      //to list all queues
      //listQueues(sqsClient);

      //deleteSQSQueue(sqsClient, queueName1); 
      //deleteSQSQueue(sqsClient, queueName2);
      String message_content = RetrieveMessage(sqsClient, Url);
    //System.out.println(message_content.length());

      String[] output = message_content.split("[,]", 0);
      String bucketName = output[0]; // bucket.emse.cloud.project.final
      String keyName = output[1]; // sales-2021-01-02.csv
      System.out.println(bucketName);
      System.out.println(keyName);

      sqsClient.close();

      String path = "C:\\Users\\adminlocal\\Desktop\\Lab3-CloudComputing\\downloaded.csv";
    
      //initializing s3 client &  region
      Region region = Region.US_EAST_1;
      S3Client s3 = S3Client.builder()
                       .region(region)
                       .build();
    
      getObjectBytes(s3,bucketName,keyName, path);

      //read from csv
      String line = "";    
      String[] sold = new String[999]; 
      String[] countries = new String[999]; 
      String[] products = new String[999];
      int i = 0;
      int j = 0;
      int total_sold = 0;
      int total_number=0;
     

      //parsing a CSV file into BufferedReader class constructor 
      try{  
           
          BufferedReader br = new BufferedReader(new FileReader(path)); 
        
          //reading all lines
          while ((line = br.readLine()) != null){
              
              // usinsg space as separator
              String[] index = line.split(",");    
              sold[i]=index[3]; 
              
              countries[i]= index[8];
              products[i]=index[2];
               
              i++;
          }    
          
          //total number amount
          total_number = sold.length-1;
          System.out.println ("total number sold: "+ total_number);

          //total sold amount
          for(i=1;i<=998;i++) {	 
             total_sold = total_sold + Integer.parseInt(sold[i]);
          } 
          System.out.println( "total amount sold: " + total_sold);

          //array of unique countries
          List<String> centerList = Arrays.asList(countries);
          Set<String> unique_countries = new HashSet<String>();
          unique_countries = new HashSet<String>();
          unique_countries.addAll(centerList);   
          String[] unique = unique_countries.toArray(new String[unique_countries.size()]);
          int [] per_country = new  int [unique.length];
          int [] counter = new  int [unique.length];
          int [] average_per_country = new  int [unique.length];

          //array of unique products
          List<String> centerList2 = Arrays.asList(products);
          Set<String> unique_products = new HashSet<String>();
          unique_products= new HashSet<String>();
          unique_products.addAll(centerList2);   
          String[] unique2 = unique_products.toArray(new String[unique_products.size()]);
          int [] per_product = new  int [unique2.length];
          int [] counter2 = new  int [unique2.length];
          int [] average_per_product = new  int [unique2.length];
          
        for(j=0;j<unique.length;j++){  
          counter[j]=0;
        }
 
        for(i=1;i<=998;i++) {
  
             //average per country
             for(j=0;j<unique.length;j++){
                    
                  if(countries[i].equals(unique[j])){
                    per_country[j] = per_country[j] + Integer.parseInt(sold[i]);
                    counter[j]++;
                }
            }
            //average per product
            for(j=0;j<unique2.length;j++){
                    
                if(products[i].equals(unique2[j])){
                  per_product[j] = per_product[j] + Integer.parseInt(sold[i]);
                  counter2[j]++;
              }
          }
            
        } 

        for(i=0;i<unique.length;i++){
            if(counter[i]!=0){
                average_per_country[i] = per_country[i]/counter[i];
            }
           // System.out.println(unique[i]+": "+per_country[i]+": "+ average_per_country[i]+ ": "+counter[i]);
        }

        for(i=0;i<unique2.length;i++){
            if(counter2[i]!=0){
                average_per_product[i] = per_product[i]/counter2[i];
            }
            //System.out.println(unique2[i]+": "+per_product[i]+": "+ average_per_product[i]+": "+ counter2[i]);
        }

        //write to csv
        String objectKey2 = "result.csv";
        String objectpath = "C:\\Users\\adminlocal\\Desktop\\Lab3-CloudComputing\\result.csv";
        writeDataLineByLine(objectpath,total_number, total_sold,average_per_country,unique,average_per_product,unique2);

        //upload file to S3
        System.out.println("Putting object " + objectKey2 +" into bucket "+bucketName);
        String result = putS3Object(s3, bucketName, objectKey2, objectpath);
        System.out.println("Tag information: "+result);

        //send message to put box que
        String message =  bucketName + "," + objectKey2;

        //initializing sqs client & region
        SqsClient sqsClient2 = SqsClient.builder()
        .region(Region.US_EAST_1)
        .build();
        sendMessage(sqsClient2, queueName2, message); //queueName2: outbox queue
        sqsClient2.close();
          
      }catch (IOException e){  
          e.printStackTrace();  
      }




  }

  public static String createQueue(SqsClient sqsClient,String queueName ) {

      try {
          System.out.println("\n"+ queueName+ " que has been created.");

          //create queue request 
          CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
              .queueName(queueName)
              .build();

          sqsClient.createQueue(createQueueRequest);
          
          //System.out.println("\nGet queue url");

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

  public static void listQueues(SqsClient sqsClient) {
	  
	  //list of queues
      System.out.println("\nList Queues: ");
      String prefix = "que";

      try {
          ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().queueNamePrefix(prefix).build();
          ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);

          for (String url : listQueuesResponse.queueUrls()) {
            //  System.out.println(url);
          }

      } catch (SqsException e) {
          System.err.println(e.awsErrorDetails().errorMessage());
          System.exit(1);
      }
  }

  public static void deleteSQSQueue(SqsClient sqsClient, String queueName) {

        try {

            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();

            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
            
            System.out.println("\nQueue has been deleted");
            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();

            sqsClient.deleteQueue(deleteQueueRequest);

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

    public static void writeDataLineByLine(String filePath, 
                                           int total_number, 
                                           int total_sold,
                                           int [] average_per_country,
                                           String [] unique,
                                           int [] average_per_product,
                                           String [] unique2
                                           ){
        // first create file object for file placed at location
        // specified by filepath
        
        File file = new File(filePath);
        try {
            // create FileWriter object with file as parameter
            FileWriter result = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(result);

            // adding header to csv
            String[] header = { "Total Number of Sales", "Total Amount Sold","Country","Average per country","Product", "Average per product"};
            writer.writeNext(header);

            Integer total_number1 = new Integer(total_number);
            Integer total_sold1 = new Integer(total_sold);

            Integer[] average_per_country1 = new Integer[average_per_country.length];
            Integer[] average_per_product1 = new Integer[average_per_product.length];
            int i = 0;
            for(i=0;i<average_per_country.length;i++){
                average_per_country1[i] = average_per_country[i];
            }
            for(i=0;i<average_per_product.length;i++){
                average_per_product1[i] = average_per_product[i];
            }
            // add data to csv    
            String[] data1 = { total_number1.toString(), total_sold1.toString(),unique[0],average_per_country1[0].toString(),unique2[0],average_per_product1[0].toString()};
            writer.writeNext(data1);

            for(i=1;i<average_per_country.length;i++){
                if(i<10){
                    String[] data2 = {"","",unique[i],average_per_country1[i].toString(),unique2[i],average_per_product1[i].toString()};
                    writer.writeNext(data2);
                }else{
                    String[] data2 = {"","",unique[i],average_per_country1[i].toString(),"",""};
                    writer.writeNext(data2); 
                }

            }

            // closing writer connection
            writer.close();
	}
	catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
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
            System.out.println("Message has been sent");
  
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

}

