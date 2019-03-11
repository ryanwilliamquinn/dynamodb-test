import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dynamo {

    private static void setupTable(DynamoDB dynamoDB, String tableName) throws InterruptedException {

        Table table = dynamoDB.createTable(tableName,
                Arrays.asList(new KeySchemaElement("containerid", KeyType.HASH),
                        new KeySchemaElement("rowid", KeyType.RANGE)),
                Arrays.asList(new AttributeDefinition("containerid", ScalarAttributeType.S),
                                new AttributeDefinition("rowid", ScalarAttributeType.S)),
                new ProvisionedThroughput(10L, 10L));
        table.waitForActive();
    }

    private static void initializeData(DynamoDB dynamoDB, String tableName) {
        Table table = dynamoDB.getTable(tableName);
        for (int containerid = 0; containerid < 1000; containerid++) {
            for (int rowid = 0; rowid < 10; rowid++) {
                LocalDateTime dateTime = LocalDateTime.now().minusMinutes(containerid + rowid);

                PutItemOutcome outcome = table.putItem(new Item()
                        //                    .withPrimaryKey("containerid", "abc" + containerid, "createdAt", dateTime.format(DateTimeFormatter.ISO_DATE_TIME))
                        .withPrimaryKey("containerid", "abc" + containerid, "rowid", rowid + "")
                        .with("data", "this is my comment " + containerid + " " + rowid));
            }
        }
    }

    public static void main(String[] args) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "us-west-2"))
                .build();


        /*
        CLI command to delete the table: aws dynamodb delete-table --table-name comments --endpoint http://localhost:8001
        Hash Key: container/grid it. Sort Key: rowid#threadid#commentid
        Things I'm finding:
        1. It is still hard to figure out how to deal with big report queries. We will be making lots of queries that
            will return no data since we will need to query row by row (unless we pull back all rows for a backing grid,
            but that seems wasteful). There is a 100 item return limit, so we will have to do some batching of requests
            on the application side.
        2. It is unclear to me how we will mark discussions vs comments, but it seems important. Maybe just prepend "root"
            to the sort key? We will need to fetch discussions somehow, to show the row indicators, but we will
            not want to return all of the related comments until the rows are selected. Filtering is expensive in dynamodb
            since it operates on the result after it is returned. If discussions don't have any data, we could make their sort key different:
            "thread"#rowid#threadid

         */


        String tableName = "comments";
        try {
            DynamoDB dynamoDB = new DynamoDB(client);
            setupTable(dynamoDB, tableName);
            System.out.println(client.listTables());
            initializeData(dynamoDB, tableName);
            Table table = dynamoDB.getTable(tableName);
//            GetItemSpec spec = new GetItemSpec().withPrimaryKey("containerid", "abc5", "rowid", "2019-03-03T17:55:03.501");
//            TableKeysAndAttributes commentKeys = new TableKeysAndAttributes(tableName)
//                    .withPrimaryKeys(new PrimaryKey("containerid", "abc5"));
//            BatchGetItemOutcome outcome = dynamoDB.batchGetItem(commentKeys);
            DynamoDBMapper mapper = new DynamoDBMapper(client);
            Map<String, AttributeValue> eav = new HashMap<>();
            eav.put(":val1", new AttributeValue().withS("abc5"));
            DynamoDBQueryExpression<Comment> queryExpression = new DynamoDBQueryExpression<Comment>()
                    .withKeyConditionExpression("containerid = :val1").withExpressionAttributeValues(eav);
            List<Comment> comments = mapper.query(Comment.class, queryExpression);
            System.out.println(comments.size());

        } catch (Exception e) {
            System.out.println("exception: " + e.getMessage());
        }

    }
}
