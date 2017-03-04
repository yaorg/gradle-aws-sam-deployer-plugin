package com.fieldju.gradle.plugins.lambdasam.services.cloudformation

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException
import com.amazonaws.services.cloudformation.model.CreateChangeSetRequest
import com.amazonaws.services.cloudformation.model.CreateChangeSetResult
import com.amazonaws.services.cloudformation.model.DescribeChangeSetRequest
import com.amazonaws.services.cloudformation.model.DescribeChangeSetResult
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.model.ExecuteChangeSetRequest
import com.amazonaws.services.cloudformation.model.ExecuteChangeSetResult
import com.amazonaws.services.cloudformation.model.Parameter
import com.amazonaws.waiters.FixedDelayStrategy
import com.amazonaws.waiters.MaxAttemptsRetryStrategy
import com.amazonaws.waiters.PollingStrategy
import com.amazonaws.waiters.SdkFunction
import com.amazonaws.waiters.Waiter
import com.amazonaws.waiters.WaiterBuilder
import com.amazonaws.waiters.WaiterParameters
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.waiters.ChangeSetCreateComplete
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.Executors

/**
 * A Service based off of the AWS CLI CloudFormation Deploy command deployer
 * see: https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/deployer.py
 */
class CloudFormationDeployer {

    Logger logger = Logging.getLogger(getClass())

    private final AmazonCloudFormationClient amazonCloudFormation

    CloudFormationDeployer(AmazonCloudFormationClient amazonCloudFormation) {
          this.amazonCloudFormation = amazonCloudFormation
     }

    /**
     * Checks if a CloudFormation stack with given name exists
     *
     * @param stackName Name or ID of the stack
     * @return True if stack exists. False otherwise
     */
    boolean hasStack(String stackName) {
        def res
        try {
            res = amazonCloudFormation.describeStacks(new DescribeStacksRequest().withStackName(stackName))
        } catch (AmazonCloudFormationException e) {
            if (e.statusCode == 400 && e.errorMessage.endsWith("does not exist")) {
                return false
            } else {
                throw e
            }
        }

        if (res.getStacks().size() != 1) {
            return false
        }

        // When you run CreateChangeSet on a a stack that does not exist,
        // CloudFormation will create a stack and set it's status
        // REVIEW_IN_PROGRESS. However this stack is cannot be manipulated
        // by "update" commands. Under this circumstances, we treat like
        // this stack does not exist and call CreateChangeSet will
        // ChangeSetType set to CREATE and not UPDATE.
        def stack = res.getStacks().get(0)
        return stack.getStackStatus() != "REVIEW_IN_PROGRESS"
    }

    /**
     * Call CloudFormation to create a change set and wait for it to complete
     *
     * @param stackName Name or ID of stack
     * @param samTemplate CloudFormation template string
     * @param parameters Template parameters object
     * @param capabilities Array of capabilities passed to CloudFormation
     * @return The create change set result
     */
    ChangeSetMetadata createChangeSet(String stackName,
                                      String samTemplate,
                                      Collection<Parameter> parameters,
                                      Collection<String> capabilities) {

        String changeSetType = hasStack(stackName) ? "UPDATE" : "CREATE"

        logger.lifecycle("Creating change set for stack: ${stackName} with change set type: ${changeSetType}")

        try {
            CreateChangeSetResult result = amazonCloudFormation.createChangeSet(
                    new CreateChangeSetRequest()
                            .withChangeSetName("gradle-lambdasam-plugin-deploy-${UUID.randomUUID().toString()}")
                            .withStackName(stackName)
                            .withTemplateBody(samTemplate)
                            .withChangeSetType(changeSetType)
                            .withParameters(parameters)
                            .withCapabilities(capabilities)
                            .withDescription("Created by AWS SAM Gradle plugin at ${ZonedDateTime.now(ZoneOffset.UTC)} UTC")
            )

            return new ChangeSetMetadata([name: result.id, type: changeSetType])
        } catch (Throwable t) {
            throw new GradleException("Failed to create changeSet", t)
        }
    }

    /**
     * Waits until the changeSet creation completes
     *
     * @param changeSetName ID or name of the changeSet
     * @param stackName Name or ID of the stack
     */
    def waitForChangeSet(String changeSetName, String stackName) {
        logger.lifecycle("Waiting for change set to be created..")
        Waiter<DescribeChangeSetRequest> waiter = new WaiterBuilder<DescribeChangeSetRequest, DescribeChangeSetResult>()
                .withSdkFunction(
                    new SdkFunction<DescribeChangeSetRequest, DescribeChangeSetResult>() {
                        @Override
                        DescribeChangeSetResult apply(DescribeChangeSetRequest describeChangeSetRequest) {
                            amazonCloudFormation.describeChangeSet(describeChangeSetRequest)
                        }
                    }
                )
                .withAcceptors(
                    new ChangeSetCreateComplete.IsCREATE_COMPLETEMatcher(),
                    new ChangeSetCreateComplete.IsFAILEDMatcher()
                )
                .withDefaultPollingStrategy(new PollingStrategy(new MaxAttemptsRetryStrategy(120), new FixedDelayStrategy(10)))
                .withExecutorService(Executors.newFixedThreadPool(50)).build()

        try {
            waiter.run(
                    new WaiterParameters<DescribeChangeSetRequest>(
                            new DescribeChangeSetRequest()
                                    .withChangeSetName(changeSetName)
                                    .withStackName(stackName)
                    )
            )
        } catch (Throwable t) {
            throw new GradleException("Failed to wait for changeset: ${changeSetName}", t)
        }
    }

    /**
     * Calls CloudFormation to execute changeSet
     *
     * @param changeSetName ID of the changeSet
     * @param stackName Name or ID of the stack
     * @return Response from execute-change-set call
     */
    ExecuteChangeSetResult executeChangeSet(String changeSetName, String stackName) {
        return amazonCloudFormation.executeChangeSet(
                new ExecuteChangeSetRequest()
                        .withChangeSetName(changeSetName)
                        .withStackName(stackName)
        )
    }

    /**
     * Blocks and waits for the execution of a changeSet
     *
     * @param changeSetType CREATE or Update
     * @param stackName Name or ID of stack
     */
    void waitForExecute(String changeSetType, String stackName) {
        logger.lifecycle("Waiting for stack create/update to complete")
        Waiter<DescribeStacksRequest> waiter
        if (changeSetType == "CREATE") {
            // wait for stack_create_complete
            waiter = amazonCloudFormation.waiters().stackCreateComplete()
        } else if (changeSetType == "UPDATE") {
            // wait for stack_update_complete
            waiter = amazonCloudFormation.waiters().stackUpdateComplete()
        } else {
            throw new GradleException("Invalid changeSet type  ${changeSetType}")
        }

        try {
            waiter.run(new WaiterParameters<DescribeStacksRequest>(new DescribeStacksRequest().withStackName(stackName)))
        } catch (Throwable t) {
            throw new GradleException("Failed to wait for the execution of the changeSet for stack: ${stackName}", t)
        }
    }

    /**
     * Call CloudFormation to create a changeSet and wait for it to complete
     *
     * @param stackName Name or ID of stack
     * @param samTemplate CloudFormation template string
     * @param parameters Template parameters
     * @param capabilities capabilities passed to CloudFormation
     * @return The create change set result
     */
    ChangeSetMetadata createAndWaitForChangeSet(String stackName,
                                                String samTemplate,
                                                Collection<Parameter> parameters,
                                                Collection<String> capabilities) {

        def result = createChangeSet(stackName, samTemplate, parameters, capabilities)
        waitForChangeSet(result.name, stackName)
        return result
    }

    /**
     * Logs the changes in a change set
     *
     * @param changeSetName ID of the changeSet
     * @param stackName Name or ID of stack
     */
    void logChangeSetDescription(String changeSetName, String stackName) {
        def res = amazonCloudFormation.describeChangeSet(
                new DescribeChangeSetRequest()
                        .withChangeSetName(changeSetName)
                        .withStackName(stackName)
        )

        logger.lifecycle("The change set: ${changeSetName} for stack: ${stackName} will execute the following changes.")
        res.changes.each { change ->
            logger.lifecycle(change.toString())
        }
    }

    class ChangeSetMetadata {
        String name, type
    }
}
