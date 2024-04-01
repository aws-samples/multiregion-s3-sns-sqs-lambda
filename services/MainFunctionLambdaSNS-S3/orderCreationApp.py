import boto3
import sns_extended_client
import json
import os

sns_extended_client = boto3.client("sns")
sqs_client = boto3.client('sqs')
s3_client = boto3.client('s3')
s3_extended_payload_bucket = os.environ["s3_extended_payload_bucket"]
TOPIC_NAME = os.environ["TOPIC_NAME"]
demo_topic_arn = os.environ['demo_topic_arn']

def lambda_handler(event,context):
    try:
        DEFAULT_MESSAGE_SIZE_THRESHOLD=256000
        LARGE_MSG_BODY = "x" * (DEFAULT_MESSAGE_SIZE_THRESHOLD + 1)

        sns_extended_client.large_payload_support = s3_extended_payload_bucket
        sns_extended_client.always_through_s3 = True
        sns_extended_client.message_size_threshold = 256000
        response = sns_extended_client.publish(
            TopicArn=demo_topic_arn, Message="DT"+LARGE_MSG_BODY , MessageGroupId="message", MessageDeduplicationId="largeMessage",
        )
        response = sns_extended_client.publish(
            TopicArn=demo_topic_arn, MessageGroupId="message", MessageDeduplicationId="smallMessage",
            Message="A normal PAYLOAD <256KB"
        )        
            
        print('request',response)
        return {
            'statusCode': 200,
            'output': 'JSON data processed and sent to downstream',
            'body':json.dumps(response)
        }
            
    except Exception as e:
        return {
            'statusCode': 500,
            'body': f'Error: {str(e)}'
        }        
        