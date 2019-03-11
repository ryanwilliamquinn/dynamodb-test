import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "comments")
public class Comment {
    private String containerid;
    private String rowid;
    private String data;

    @DynamoDBHashKey(attributeName = "containerid")
    public String getContainerId() {
        return containerid;
    }

    public void setContainerId(String containerid) {
        this.containerid = containerid;
    }

    @DynamoDBRangeKey(attributeName = "rowid")
    public String getRowid() {
        return rowid;
    }

    public void setRowid(String rowid) {
        this.rowid = rowid;
    }

    @DynamoDBAttribute(attributeName = "data")
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String toString() {
       return data + " - " + rowid + ", " + containerid;
    }
}
