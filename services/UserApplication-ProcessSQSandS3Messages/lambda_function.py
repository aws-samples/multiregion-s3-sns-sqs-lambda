import json
import boto3
import os
import ast

s3_client = boto3.client('s3')
dynamodb = boto3.client('dynamodb')
sqs = boto3.client('sqs')

s3_bucket_name = os.environ["s3_bucket_name"]
dynamodb_table_name = os.environ["dynamodb_table_name"]
queue_url = os.environ['queue_url']
infrequent_storage_class = os.environ['infrequent_storage_class']

def lambda_handler(event, context):
    for record in event['Records']:
        print(record)
        payload = record["body"]
        converted = ast.literal_eval(payload)
        print("converted Payload",converted)
        #messageIdFromSQS = record["messageId"]
        try:
            bucketKey = converted[1].get("s3Key")
            print("bucketKey == ",bucketKey)

            print("checking whether record already presents..")

            checkItem = dynamodb.get_item(
                TableName=dynamodb_table_name,
                Key={'messageId': {'S': bucketKey}})

            #print("checkItem",checkItem) #getting requestid
            itempresent = checkItem.get('Item')

            if itempresent:
                print("Previous record found, deleting it from queue!")
                try:
                    receipt_handle = event['Records'][0]['receiptHandle']
                    sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=receipt_handle)

                    print(f"Message with receipt handle {receipt_handle} deleted successfully.")
                except Exception as e:
                    print(f"Error deleting message: {str(e)}")

            else:
                print("No previous record found, continuing to process...")
                try:
                    objectList = s3_client.list_objects(Bucket=s3_bucket_name)
                    print(objectList)

                    try:
                        for obj in objectList['Contents']:
                            #print("object in S3 ==",obj['Key'])
                            if(obj['Key']==bucketKey):
                                #print("Object found")
                                s3Object = s3_client.get_object(Bucket=s3_bucket_name,Key=bucketKey)

                                #print("s3Object",s3Object)
                                actualmsg = s3Object.get("Body").read().decode()

                                print("actualmsg",actualmsg) #only for test purpose

                                data = dynamodb.put_item(
                                    TableName=dynamodb_table_name,
                                    Item={'messageId':{'S':obj['Key']}}
                                )
                                print("Record added in Database is ", obj['Key'])
                                try:
                                    receipt_handle = event['Records'][0]['receiptHandle']
                                    sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=receipt_handle)
                                    print(f"Message with receipt handle {receipt_handle} deleted successfully.")
                                except Exception as e:
                                    print(f"Error deleting message: {str(e)}")

                                try:
                                    s3_client.delete_object(Bucket=s3_bucket_name, Key=obj['Key'])
                                    print("object deleted from ", s3_bucket_name)
                                except Exception as e:
                                    print(f"error while deleting object from S3: {str(e)}")
                    except Exception as e:
                        print(f"error inside Processing Message from S3: {str(e)}")
                except Exception as e:
                    print(f"error inside listing S3 objects: {str(e)}")
        except Exception as e:
            print(f"error in main try: {str(e)}")
    return {
        'statusCode': 200,
        'body': json.dumps('successfully updated item!')
    }