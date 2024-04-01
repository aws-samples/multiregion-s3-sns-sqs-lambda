package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.constructs.Construct;

import java.util.List;

public class DynamoDBStack extends Construct {
    private static TableV2 dynamoDBTable;

    public DynamoDBStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id);

        dynamoDBTable = TableV2.Builder.create(this, "MydynamoDBTable")
                .tableName(Multiregionwithextendedlib.DYNAMODB_TABLE_NAME)
                .partitionKey(Attribute.builder().name("messageId").type(AttributeType.STRING).build())
                .removalPolicy(RemovalPolicy.DESTROY)
                .replicas(List.of(ReplicaTableProps.builder().region(Multiregionwithextendedlib.REGION).build()))
                .build();

        CfnOutput.Builder.create(this, "DynamoDBTableOutput")
                .description("DynamoDB Table")
                .value(dynamoDBTable.getTableName())
                .exportName("DynamoDBTableName")
                .build();
    }

    public static String getDynamoDBTableName() {
        return dynamoDBTable.getTableName();
    }

    public static TableV2 getDynamoDBTable() {
        return dynamoDBTable;
    }
}
