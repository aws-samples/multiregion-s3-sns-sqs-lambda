package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.TableV2;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;


public class MultiregionwithextendedlibStack extends Stack {
    public MultiregionwithextendedlibStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MultiregionwithextendedlibStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        SNSTopicSQSStack snsTopicObj = new SNSTopicSQSStack(this, "MySNSTopicStack");
        Topic snsTopic = snsTopicObj.getTopic();
        Queue sqsQueue = snsTopicObj.getQueue();

        S3BucketStack s3BucketStack = new S3BucketStack(this, "MyEncryptedBucketStack", BucketProps.builder().build());
        Bucket myBucket = s3BucketStack.getBucket();

        DynamoDBStack dynamoDBTableStack = new DynamoDBStack(this, "MydynamoDBTable", StackProps.builder().build());
        TableV2 dynamoDBTable = dynamoDBTableStack.getDynamoDBTable();

        final Function UserApplicationlambdaFunction = Function.Builder.create(this, "UserApplication")
                .runtime(Runtime.PYTHON_3_11)
                .code(Code.fromAsset("services/UserApplication-ProcessSQSandS3Messages"))
                .handler("lambda_function.lambda_handler")
                .tracing(Tracing.ACTIVE)
                .environment(Map.of("queue_url", sqsQueue.getQueueUrl(), "dynamodb_table_name",dynamoDBTable.getTableName(),
                        "s3_bucket_name", myBucket.getBucketName(), "infrequent_storage_class", "STANDARD_IA"))
                .memorySize(1024)
                .timeout(Duration.seconds(5))
                .functionName("OrderProcessingApp")
                .build();

        sqsQueue.grantConsumeMessages(UserApplicationlambdaFunction);
        sqsQueue.grantSendMessages(UserApplicationlambdaFunction);
        dynamoDBTable.grantReadWriteData(UserApplicationlambdaFunction);
        myBucket.grantReadWrite(UserApplicationlambdaFunction);


        UserApplicationlambdaFunction.addEventSource(SqsEventSource.Builder.create(sqsQueue)
                .build());

//        LayerVersion layer = LayerVersion.Builder.create(this, "MyPythonLayer")
//                .code(Code.fromAsset("services/layer/python.zip"))
//                .compatibleArchitectures(Arrays.asList(Architecture.X86_64))
//                .compatibleRuntimes(Arrays.asList(Runtime.PYTHON_3_11))
//                .removalPolicy(RemovalPolicy.DESTROY)
//                .build();

        final Function MainlambdaFunction = Function.Builder.create(this, "MainlambdaFunction")
                .runtime(Runtime.PYTHON_3_11)
                .code(Code.fromAsset("services/MainFunctionLambdaSNS-S3"))
                .handler("orderCreationApp.lambda_handler")
                .tracing(Tracing.ACTIVE)
                .environment(Map.of("TOPIC_NAME", snsTopic.getTopicName(),
                        "s3_extended_payload_bucket", myBucket.getBucketName(),
                        "demo_topic_arn", snsTopic.getTopicArn()))
                .functionName("orderCreationApp")
                .build();

        snsTopic.grantPublish(MainlambdaFunction);
        myBucket.grantReadWrite(MainlambdaFunction);

    }
}
