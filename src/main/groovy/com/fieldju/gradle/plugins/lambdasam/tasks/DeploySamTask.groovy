package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input

class DeploySamTask extends SamTask {

    @Input
    String stackName

    @Input
    Map<String, String> parameterOverrides

    @Input
    String templatePath = "${project.buildDir.absolutePath}${File.separator}sam${File.separator}sam-deploy.yaml"

    @Input
    boolean executeChangeSet = true

    DeploySamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @Override
    void taskAction() {
        CloudFormationDeployer deployer = new CloudFormationDeployer(
                AmazonCloudFormationClient.builder()
                        .standard()
                        .withRegion(Regions.fromName(region))
                        .build() as AmazonCloudFormationClient
        )

        deployer.deployStack(getStackName(), templatePath, parameterOverrides, executeChangeSet)
    }



    private String getStackName() {
        if (stackName == null || stackName == "") {
            throw new GradleException("${stackName} is a required property")
        }
        return stackName
    }
}
