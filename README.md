# Oracle Functions + OCI Streaming Service

This is an example which shows how you can invoke [Oracle Cloud Infrastructure Streaming Service](https://docs.cloud.oracle.com/iaas/Content/Streaming/Concepts/streamingoverview.htm) from Oracle Functions. A Java function acts as a producer and pushes messages to the Streaming Service using APIs in the [OCI Java SDK](https://docs.cloud.oracle.com/iaas/Content/API/SDKDocs/javasdk.htm) with help of Resource Principal authentication provider.

## Pre-requisites

- Streaming Service: You need to create a Stream - please refer to the [details in the documentation](https://docs.cloud.oracle.com/iaas/Content/Streaming/Tasks/managingstreams.htm).
- Ensure you are using the latest version of the Fn CLI. To update simply run the following command - `curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh`
- Oracle Functions: Configure the Oracle Functions service along with your development environment and switch to the correct Fn context using `fn use context <context-name>` 

Last but not the least, clone (`git clone https://github.com/abhirockzz/fn-streaming-resource-principals`) or download this repository before proceeding further

## Create & deploy an application

Create an application with required configuration

        fn create app --annotation oracle.com/oci/subnetIds='["<OCI_SUBNET_OCIDs>"]' --config STREAM_REGION=<OCI_REGION> fn-streaming-app

> `STREAM_REGION` - Streaming Service region e.g. `us-phoenix-1`

To deploy

        fn -v deploy --app fn-streaming-app
 
Get the function OCID

        fn inspect fn fn-streaming-app streaming-producer id

## Functions Resource Principal configuration

### Dynamic Group

Add a dynamic group (e.g. `functions-dynamic-group`) with a rule to allow the function you just deployed to be added to the group

        resource.id='<FUNCTION_OCID>'
        
        //example
        resource.id='ocid1.fnfunc.oc1.phx.aaaaaaaaaduxxplcey6xjxhhpqwif7zerqwl26h2wqecywtttgp4gdqe4fna'

### IAM Policy

Add a policy (e.g. `functions-publish-to-stream-policy`) to allow function to publish to a specific stream in a specific compartment

        allow dynamic-group <DYNAMIC_GROUP_NAME> to use stream-pull in compartment <COMPARTMENT_NAME> where target.stream.id = '<STREAM_OCID>'

## Testing

The function expects a payload with the following info - compartment OCID (where stream exists), stream name, `key` (String) and a `value` (String)

`echo -n '{"streamName":"test-stream", "compartmentOCID":"ocid1.compartment.oc1..aaaaaaaaokbzj2jn3hf5kwdwqoxl2dq7u54p3tsmxrjd7s3uu7x23tfoobar","key":"hello","value":"world"}' | fn invoke fn-streaming-app streaming-producer`

If successful, you should get an output similar to this

`Message pushed to offset 42 in partition 3`