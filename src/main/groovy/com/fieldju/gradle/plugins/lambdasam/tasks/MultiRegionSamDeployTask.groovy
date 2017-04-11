package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.fieldju.commons.StringUtils
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class MultiRegionSamDeployTask extends DefaultTask {

    @Input
    @Optional
    String templatePath

    @Input
    @Optional
    def regionTemplatePathMap = [:]

    @Input
    def regions = []

    @Input
    String stackName

    @Input
    Map<String, Map<String, String>> regionToParameterOverridesMap = [:]

    @Input
    boolean executeChangeSet = true

    @TaskAction
    void taskAction() {
        regions.each { String region ->
            Map<String, String> parameterOverrides
            if (regionToParameterOverridesMap.containsKey(region)) {
                parameterOverrides = regionToParameterOverridesMap."${region}"
            } else {
                logger.lifecycle("regionToParameterOverridesMap does not contain an entry for region ${region} defaulting to empty map")
                parameterOverrides = [:]
            }

            String templatePath
            if (StringUtils.isNotBlank(this.templatePath)) {
                templatePath = this.templatePath
            } else if (regionTemplatePathMap.containsKey(region)) {
                templatePath = regionTemplatePathMap."${region}"
            } else {
                throw new GradleException("templatePath or regionTemplatePathMap.'\${region}' for " +
                        "region: ${region} must be set")
            }

            CloudFormationDeployer deployer = new CloudFormationDeployer(
                    AmazonCloudFormationClient.builder()
                            .standard()
                            .withRegion(Regions.fromName(region))
                            .build() as AmazonCloudFormationClient
            )

            logger.lifecycle("Creating and executing changeset for ${templatePath} with stackname ${stackName} in region ${region} with overrides ${parameterOverrides}")
            deployer.deployStack(stackName, templatePath, parameterOverrides, executeChangeSet)
        }
    }
}
