package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.CfnBucket;
import software.amazon.awscdk.services.s3.CfnBucket.ReplicationConfigurationProperty;
import software.amazon.awscdk.services.s3.CfnBucket.ReplicationDestinationProperty;
import software.amazon.awscdk.services.s3.CfnBucket.ReplicationRuleProperty;
import software.constructs.Construct;

import java.util.List;


public class S3BucketStack extends Construct {
    private final Bucket bucket;
    public S3BucketStack(final Construct parent, final String id) {
        this(parent, id, null);
    }

    public S3BucketStack(final Construct parent, final String id, final BucketProps props) {
        super(parent,id);

        bucket = Bucket.Builder.create(this, "MyBucket")
                .bucketName(Multiregionwithextendedlib.BUCKET_NAME)
                .encryption(BucketEncryption.S3_MANAGED)
                .versioned(true)
                .removalPolicy(RemovalPolicy.DESTROY)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        Role replicationRole = Role.Builder.create(this, "ReplicationRole")
                .assumedBy(new ServicePrincipal("s3.amazonaws.com"))
                .build();

        replicationRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess")); //only for test purpose

        ReplicationConfigurationProperty replicationConfigurationProperty = ReplicationConfigurationProperty.builder()
                .role(replicationRole.getRoleArn())
                .rules(List.of(ReplicationRuleProperty.builder()
                        .destination(ReplicationDestinationProperty.builder()
                                .bucket("arn:aws:s3:::"+ Multiregionwithextendedlib.BUCKET_NAME+"-secondary")
                                .build())
                        .status("Enabled")
                        .build()))
                .build();

        ((CfnBucket)bucket.getNode().getDefaultChild()).setReplicationConfiguration(replicationConfigurationProperty);

        CfnOutput.Builder.create(this, "BucketName")
                .value(bucket.getBucketName())
                .exportName("BucketName")
                .build();
    }

    public Bucket getBucket() {
        return bucket;
    }


}
