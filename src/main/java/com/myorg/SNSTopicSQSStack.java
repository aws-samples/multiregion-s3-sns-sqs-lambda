package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.constructs.Construct;
import software.amazon.awscdk.services.sns.*;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;

import java.util.Collections;

public class SNSTopicSQSStack extends Construct {
    private final Topic snsTopic;
    private final  Queue queue;
    public SNSTopicSQSStack(final Construct scope, final String id) {
        super(scope, id);

//        DeadLetterQueue.Builder deadLetterQueue1 = DeadLetterQueue.builder()
//                .maxReceiveCount(10)
//                .queue(Queue.Builder.create(this, "MySQSQueueDLQ").fifo(true).queueName("MySQSQueueDLQ.fifo").build());

        snsTopic = Topic.Builder.create(this, "MySNSTopic")
                .topicName("MySNSTopic.fifo")
                .fifo(true)
                .build();

        queue = Queue.Builder.create(this, "MySQSQueue")
                .fifo(true)
                .queueName("MySQSQueue.fifo")
                .removalPolicy(RemovalPolicy.DESTROY)
                .visibilityTimeout(Duration.seconds(50))
                .deliveryDelay(Duration.seconds(40))
                .build();

        snsTopic.addSubscription(new SqsSubscription(queue));

        //IQueue subscribeToAnotherSQS = Queue.fromQueueArn(this, "MySQSQueue1", "arn:aws:sqs:us-east-2:673824674024:MySQSQueue.fifo");
        //snsTopic.addSubscription(new SqsSubscription(subscribeToAnotherSQS));

        PolicyStatement policyStatement = PolicyStatement.Builder.create()
                .actions(Collections.singletonList("SQS:SendMessage"))
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList(queue.getQueueArn()))
                .principals(Collections.singletonList(new ServicePrincipal("sns.amazonaws.com")))
                .build();

        queue.addToResourcePolicy(policyStatement);

        Subscription.Builder.create(this, "MySubscription")
                .rawMessageDelivery(true)
                .endpoint(queue.getQueueArn())
                .protocol(SubscriptionProtocol.SQS)
                .topic(snsTopic)
                .build();

        Subscription.Builder.create(this, "MySubscription2")
                .rawMessageDelivery(true)
                .endpoint("arn:aws:sqs:"+ Multiregionwithextendedlib.REGION+":"+ Multiregionwithextendedlib.ACCOUNT+":MySQSQueue.fifo")
                .protocol(SubscriptionProtocol.SQS)
                .topic(snsTopic)
                .build();

        CfnOutput.Builder.create(this, "MySNSTopicArn")
                .exportName("MySNSTopicArn")
                .value(snsTopic.getTopicArn())
                .build();

        CfnOutput.Builder.create(this, "MySQSQueueArn")
                .exportName("MySQSQueueArn")
                .value(queue.getQueueArn())
                .build();

    }

    public Topic getTopic() {
        return snsTopic;
    }
    public Queue getQueue() {
        return queue;
    }
}
