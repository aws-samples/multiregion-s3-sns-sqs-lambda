package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class MultiregionwithextendedlibappSecondaryStack extends Stack {

    public MultiregionwithextendedlibappSecondaryStack(final Construct parent, final String id, final StackProps props) {
        super(parent, id, props);

        SNSTopicSQSStack snsTopicObj1 = new SNSTopicSQSStack(this, "MySNSTopicStack");
        Topic snsTopic = snsTopicObj1.getTopic();
        Queue sqsQueue = snsTopicObj1.getQueue();


        Bucket replicatedBucket = Bucket.Builder.create(this, "ReplicatedBucket")
                .bucketName(Multiregionwithextendedlib.BUCKET_NAME + "-secondary")
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        final Function MainlambdaFunction = Function.Builder.create(this, "MainlambdaFunction")
                .runtime(Runtime.PYTHON_3_11)
                .code(Code.fromAsset("services/MainFunctionLambdaSNS-S3"))
                .handler("orderCreationApp.lambda_handler")
                .tracing(Tracing.ACTIVE)
                .environment(Map.of("TOPIC_NAME", snsTopic.getTopicName(),
                        "s3_extended_payload_bucket", replicatedBucket.getBucketName(),
                        "demo_topic_arn", snsTopic.getTopicArn()))
                .functionName("orderCreationApp")
                .build();

        snsTopic.grantPublish(MainlambdaFunction);
        replicatedBucket.grantReadWrite(MainlambdaFunction);

        Role mylambdarole = Role.Builder.create(this, "MyLambdaRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build();

        mylambdarole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));
        mylambdarole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess")); //only for test purpose

        final Function UserApplicationlambdaFunction = Function.Builder.create(this, "UserApplication")
                .runtime(Runtime.PYTHON_3_11)
                .code(Code.fromAsset("services/UserApplication-ProcessSQSandS3Messages"))
                .handler("lambda_function.lambda_handler")
                .tracing(Tracing.ACTIVE)
                .environment(Map.of("queue_url", sqsQueue.getQueueUrl(), "dynamodb_table_name", DynamoDBStack.getDynamoDBTableName(),
                        "s3_bucket_name", replicatedBucket.getBucketName(), "infrequent_storage_class", "STANDARD_IA"))
                .memorySize(1024)
                .timeout(Duration.seconds(5))
                .role(mylambdarole)
                .functionName("OrderProcessingApp")
                .build();


        sqsQueue.grantConsumeMessages(UserApplicationlambdaFunction);
        sqsQueue.grantSendMessages(UserApplicationlambdaFunction);
        replicatedBucket.grantReadWrite(UserApplicationlambdaFunction);

        UserApplicationlambdaFunction.addEventSource(SqsEventSource.Builder.create(sqsQueue)
                .build());
    }
}
