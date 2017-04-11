package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest
import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerExtension
import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction;

public abstract class SamTask extends DefaultTask {

    static final String TASK_GROUP = 'AWS Lambda SAM Deployer'

    @Input
    String region

    protected void logExtraDetails() {
        AwsSamDeployerExtension extension = project.extensions.getByName(AwsSamDeployerPlugin.EXTENSION_NAME) as AwsSamDeployerExtension
        try {
            def stsClient = AWSSecurityTokenServiceClient.builder().standard().withRegion(extension.getRegion()).build()
            def res = stsClient.getCallerIdentity(new GetCallerIdentityRequest())
            logger.lifecycle("Running Command: ${this.name} with AWS Identity info, ARN: ${res.arn}, Account: ${res.account}, UserId: ${res.userId}")
        } catch (Throwable t) {
            logger.error("Failed to get caller identity via AWS STS", t)
        }

        logger.info("extention: ${extension.toString()}")
    }

    @TaskAction
    void preTaskAction() {
        logExtraDetails()
        taskAction()
    }

    abstract void taskAction()
}
