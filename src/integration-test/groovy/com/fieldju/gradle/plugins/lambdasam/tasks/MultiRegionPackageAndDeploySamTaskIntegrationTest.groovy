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
import com.fieldju.commons.EnvUtils
import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerPlugin
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.nio.file.Files
import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail

@Slf4j
class MultiRegionPackageAndDeploySamTaskIntegrationTest {
    final static def regions = ['us-west-2','us-east-1']
    Map<String, AmazonS3> regionS3Map = [:]
    Map<String, AmazonCloudFormation> regionAmazonCloudFormationMap = [:]
    Map<String, String> regionS3BucketMap
    String testStackName
    String prefix

    @Before
    void before() {
        testStackName = "MultiRegionPackageAndDeploySamTaskIntegrationTest-${UUID.randomUUID()}"
        regionS3BucketMap = [
                "us-west-2": EnvUtils.getRequiredEnv('S3_BUCKET', 'The us-west-2 s3 bucket to upload the lambda fat jar'),
                "us-east-1": EnvUtils.getRequiredEnv('S3_BUCKET_EAST', 'The us-east-2 s3 bucket to upload the lambda fat jar')
        ]

        regions.each { regionString ->
            regionAmazonCloudFormationMap.put(regionString, AmazonCloudFormationClient.builder()
                    .standard()
                    .withRegion(regionString)
                    .build() as AmazonCloudFormationClient)
            regionS3Map.put(regionString, AmazonS3Client.builder().standard().withRegion(regionString).build())
        }

        prefix = "gradle-aws-sam-deployer-plugin-integration-test/${UUID.randomUUID().toString()}"
        log.info("Integration Test stack name: ${testStackName}")
    }

    @After
    void after() {
        log.info("Deleting Test CloudFormation Stacks")

        regions.each { region ->
            def cloudFormation = regionAmazonCloudFormationMap.get(region)
            def s3 = regionS3Map.get(region)
            def bucket = regionS3BucketMap.get(region)
            try {
                cloudFormation.deleteStack(new DeleteStackRequest().withStackName(testStackName))
                Waiter<DescribeStacksRequest> waiter = cloudFormation.waiters().stackDeleteComplete()
                waiter.run(new WaiterParameters<DescribeStacksRequest>(new DescribeStacksRequest().withStackName(testStackName)))
                log.info("Successfully deleted Test CloudFormation Stack ${testStackName} in region ${region}")

                List<String> keys = s3.listObjectsV2(bucket, prefix).getObjectSummaries().collect { it.key }
                keys.each { key ->
                    log.info("Deleting test key: ${key}")
                    s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(key))
                }
            } catch (Throwable t) {
                log.error("Failed to delete stack: ${testStackName} in region: ${region}, please ensure it gets cleaned up manually", t)
            }
        }
    }

    @Test
    void "test that the package and deploy sam tasks can deploy hello word lambda function in multiple regions"() {
        File temp = File.createTempDir()

        def samTemplateSource = Paths.get(getClass().getClassLoader().getResource('application.yaml').toURI())
        def samTemplateDest = Paths.get("${temp.absolutePath}${File.separator}application.yaml")
        def fatJarSource = Paths.get(getClass().getClassLoader().getResource('jvm-hello-world-lambda.jar').toURI())
        def fatJarDest = Paths.get("${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar")

        Files.copy(samTemplateSource, samTemplateDest)
        Files.copy(fatJarSource, fatJarDest)

        Project project = ProjectBuilder.builder().withName('DeploySamTaskIntegrationTest').withProjectDir(temp).build()
        AwsSamDeployerPlugin plugin = new AwsSamDeployerPlugin()
        plugin.apply(project)

        MultiRegionPackageAndDeploySamTask task = project.task('testTask', type: MultiRegionPackageAndDeploySamTask) as MultiRegionPackageAndDeploySamTask
        task.regions = regions
        task.stackName = testStackName
        task.templatePath = "${temp.absolutePath}${File.separator}application.yaml"
        task.regionToS3BucketMap = regionS3BucketMap
        task.s3Prefix = prefix
        task.forceUploads = true
        task.tokenArtifactMap = [
                '@@LAMBDA_FAT_JAR@@': "${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar"
        ]
        task.parameterOverrides = [
                Foo: 'bar'
        ]
        task.logStackOutputs = true

        MultiRegionPackageAndDeploySamTask testTask = project.tasks.getByName('testTask') as MultiRegionPackageAndDeploySamTask
        testTask.taskAction()

        regions.each { region ->
            try {
                def res = regionAmazonCloudFormationMap.get(region).describeStacks(new DescribeStacksRequest().withStackName(testStackName))
                assertEquals("There should be one and only one stack with the name: ${testStackName}", 1, res.stacks.size())
                assertEquals("There should be one and only one parameter", 1, res.stacks.get(0).parameters.size())
                assertEquals("The param Foo should be bar", "bar",  res.stacks.get(0).parameters.get(0).getParameterValue())
            } catch (AmazonCloudFormationException e) {
                log.error("Failed trying to describe stack: ${testStackName}", e)
                fail("Failed to assert that the stack: ${testStackName} was successfully created, msg: ${e.errorMessage}")
            }
        }
    }
}
