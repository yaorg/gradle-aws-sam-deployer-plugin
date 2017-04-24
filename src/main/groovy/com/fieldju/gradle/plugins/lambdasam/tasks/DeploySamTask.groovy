package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.fieldju.gradle.plugins.lambdasam.services.PackageAndDeployTaskHelper
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class DeploySamTask extends SamTask {

    @Input
    String stackName

    @Input
    Map<String, String> parameterOverrides

    @Input
    @Optional
    String templatePath

    @Input
    boolean executeChangeSet = true

    @Input
    boolean logStackOutputs = false

    DeploySamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @Override
    void taskAction() {
        def calculatedTemplatePath = templatePath ? templatePath : "${project.buildDir.absolutePath}${File.separator}sam${File.separator}sam-deploy-${region}.yaml"

        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)
        helper.deployProcessedTemplate(region, stackName, calculatedTemplatePath, parameterOverrides, executeChangeSet, logStackOutputs)
    }
}
