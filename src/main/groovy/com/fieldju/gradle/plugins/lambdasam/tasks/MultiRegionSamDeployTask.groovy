package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.commons.StringUtils
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class MultiRegionSamDeployTask extends SamTask {

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

    @Override
    void taskAction() {
        regions.each { region ->
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

            DeploySamTask deploySamTask = new DeploySamTask()
            deploySamTask.region = region
            deploySamTask.stackName = stackName
            deploySamTask.parameterOverrides = parameterOverrides
            deploySamTask.templatePath = templatePath

            logger.lifecycle("Creating and executing changeset for ${templatePath} with stackname ${stackName} in region ${region} with overrides ${parameterOverrides}")
            deploySamTask.taskAction()
        }
    }
}
