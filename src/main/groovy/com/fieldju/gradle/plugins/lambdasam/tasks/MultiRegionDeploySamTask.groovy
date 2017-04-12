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
    Map<String, String> regionTemplatePathMap = [:]

    @Input
    List<String> regions = []

    @Input
    String stackName

    @Input
    Map<String, Map<String, String>> regionToParameterOverridesMap = [:]

    @Input
    boolean executeChangeSet = true

    @TaskAction
    void taskAction() {
        // Groovy Strings and GStrings don't work together in collections because hashCode() produces different hashes
        // So lets make sure that we are dealing with the same types
        def regionTemplatePathMap = this.regionTemplatePathMap.collectEntries { String key, String value -> [(key.toString()): value ] }
        def regionToParameterOverridesMap = this.regionToParameterOverridesMap.collectEntries { key, value -> [(key.toString()): value ] }
        List<String> regions = this.regions*.toString()

        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)
        //noinspection GroovyAssignabilityCheck
        helper.multiRegionDeploy(templatePath, regionTemplatePathMap, regions,
                stackName, regionToParameterOverridesMap, executeChangeSet)
    }
}
