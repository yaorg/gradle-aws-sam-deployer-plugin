package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.services.PackageAndDeployTaskHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class MultiRegionDeploySamTask extends DefaultTask {

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
        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)
        helper.multiRegionDeploy(templatePath, regionTemplatePathMap, regions,
                stackName, regionToParameterOverridesMap, executeChangeSet)
    }
}
