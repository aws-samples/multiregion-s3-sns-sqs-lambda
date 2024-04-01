package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Multiregionwithextendedlib {
    static final String BUCKET_NAME = "my-unique-bucket-capstone03112023";

    static final String REGION2 = "us-west-1"; //primary region
    static final String REGION = "us-east-2"; //secondary region
    static final String ACCOUNT = "";
    static final String DYNAMODB_TABLE_NAME = "capstone-project-table";

    static Environment makeEnv(String account, String region) {
        account = (account == null) ? System.getenv("CDK_DEPLOY_ACCOUNT") : account;
        region = (region == null) ? System.getenv("CDK_DEPLOY_REGION") : region;
        return Environment.builder()
                .account(account)
                .region(region)
                .build();
    }
    public static void main(final String[] args) {
        App app = new App();
        Environment primaryEnv = makeEnv(null, REGION2);
        Environment secondaryEnv = makeEnv(null, REGION);

        new MultiregionwithextendedlibStack(app, "MultiregionwithextendedlibStack", StackProps.builder().env(primaryEnv).build());
        new MultiregionwithextendedlibappSecondaryStack(app, "MultiregionwithextendedlibappAppSecondaryStack", StackProps.builder().env(secondaryEnv).build());

        app.synth();
    }

    public String getEnv() {
        return System.getenv("CDK_DEPLOY_REGION");
    }

}

