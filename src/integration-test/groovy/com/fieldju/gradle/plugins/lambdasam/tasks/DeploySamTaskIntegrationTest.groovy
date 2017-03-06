package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException
import com.amazonaws.services.cloudformation.model.DeleteStackRequest
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.waiters.Waiter
import com.amazonaws.waiters.WaiterParameters
import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerPlugin
import groovy.util.logging.Slf4j
import org.gradle.api.logging.LogLevel
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

@Slf4j
class DeploySamTaskIntegrationTest {

    String regionString
    String testStackName
    AmazonCloudFormation cloudFormation
    AmazonS3 s3
    String prefix
    String bucket

    @Before
    void before() {
        regionString = 'us-west-2'
        testStackName = "DeploySamTaskIntegrationTest-${UUID.randomUUID()}"
        // Verify That the stack exists
        cloudFormation = AmazonCloudFormationClient.builder()
                .standard()
                .withRegion(regionString)
                .build() as AmazonCloudFormationClient

        s3 = AmazonS3Client.builder().standard().withRegion(regionString).build()

        prefix = "gradle-aws-sam-deployer-plugin-integration-test/${UUID.randomUUID().toString()}"
        bucket = getRequiredTestParam('S3_BUCKET', 'The s3 bucket to upload the lambda fat jar')
        log.info("Integration Test stack name: ${testStackName}")
    }

    @After
    void after() {
        try {
            log.info("Deleting Test CloudFormation Stack")
            cloudFormation.deleteStack(new DeleteStackRequest().withStackName(testStackName))
            Waiter<DescribeStacksRequest> waiter = cloudFormation.waiters().stackDeleteComplete()
            waiter.run(new WaiterParameters<DescribeStacksRequest>(new DescribeStacksRequest().withStackName(testStackName)))
            log.info("Successfully deleted Test CloudFormation Stack")

            List<String> keys = s3.listObjectsV2(bucket, prefix).getObjectSummaries().collect { it.key }
            keys.each { key ->
                log.info("Deleting test key: ${key}")
                s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(key))
            }
        } catch (Throwable t) {
            log.error("Failed to delete stack: ${testStackName}, please ensure it gets cleaned up manually", t)
        }
    }

    @Test
    void "test that the package and deploy sam tasks can deploy hello word lambda function"() {
        File temp = File.createTempDir()

        def samTemplateSource = Paths.get(getClass().getClassLoader().getResource('application.yaml').toURI())
        def samTemplateDest = Paths.get("${temp.absolutePath}${File.separator}application.yaml")
        def fatJarSource = Paths.get(getClass().getClassLoader().getResource('jvm-hello-world-lambda.jar').toURI())
        def fatJarDest = Paths.get("${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar")

        Files.copy(samTemplateSource, samTemplateDest)
        Files.copy(fatJarSource, fatJarDest)

        def project = ProjectBuilder.builder().withName('DeploySamTaskIntegrationTest').withProjectDir(temp).build()
        AwsSamDeployerPlugin plugin = new AwsSamDeployerPlugin()
        plugin.apply(project)
        project.'aws-sam-deployer' {
            region = regionString
            s3Bucket = bucket
            s3Prefix = prefix
            stackName = testStackName
            samTemplatePath = "${temp.absolutePath}${File.separator}application.yaml"
            tokenArtifactMap = [
                    '@@LAMBDA_FAT_JAR@@': "${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar"
            ]
            parameterOverrides = [
                    Foo: 'bar'
            ]
            forceUploads = true
        }

        // run the package sam task, since deploy depends on it
        def packageSameTask = project.tasks.getByName(AwsSamDeployerPlugin.TaskDefinitions.PACKAGE_SAM_TASK.name as String) as PackageSamTask
        packageSameTask.taskAction()
        // deploy the sam
        def deploySamTask = project.tasks.getByName(AwsSamDeployerPlugin.TaskDefinitions.DEPLOY_SAM_TASK.name as String) as DeploySamTask
        deploySamTask.taskAction()

        try {
            def res = cloudFormation.describeStacks(new DescribeStacksRequest().withStackName(testStackName))
            assertEquals("There should be one and only one stack with the name: ${testStackName}", 1, res.stacks.size())
            assertEquals("There should be one and only one parameter", 1, res.stacks.get(0).parameters.size())
            assertEquals("The param Foo should be bar", "bar",  res.stacks.get(0).parameters.get(0).getParameterValue())
        } catch (AmazonCloudFormationException e) {
            log.error("Failed trying to describe stack: ${testStackName}", e)
            fail("Failed to assert that the stack: ${testStackName} was successfully created, msg: ${e.errorMessage}")
        }
    }

    static String getRequiredTestParam(String key, String msg) {
        def value = System.getenv(key)
        if (value == null || value.trim() == "") {
            throw new RuntimeException("The environment variable: ${key} is required. Msg: ${msg}")
        }
        return value
    }

}
