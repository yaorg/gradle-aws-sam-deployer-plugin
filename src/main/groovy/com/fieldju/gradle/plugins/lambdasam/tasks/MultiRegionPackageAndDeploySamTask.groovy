package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.services.PackageAndDeployTaskHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class MultiRegionPackageAndDeploySamTask extends DefaultTask {

    @Input
    List<String> regions = []

    @Input
    String stackName

    @Input
    String templatePath

    @Input
    @Optional
    Map<String, String> regionToS3BucketMap = [:]

    @Input
    @Optional
    String s3Prefix

    @Input
    @Optional
    Map<String, String> regionToKmsKeyIdMap = [:]

    @Input
    @Optional
    boolean forceUploads

    @Input
    Map<String, String> tokenArtifactMap

    @Input
    Map<String, String> parameterOverrides = [:]

    @Input
    Map<String, Map<String, String>> regionToParameterOverridesMap = [:]

    @Input
    boolean executeChangeSet = true

    @Input
    boolean logStackOutputs = false

    @TaskAction
    void taskAction() {
        // Groovy Strings and GStrings don't work together in collections because hashCode() produces different hashes
        // So lets make sure that we are dealing with the same types
        def regionToS3BucketMap = this.regionToS3BucketMap.collectEntries { key, value -> [ (key.toString()) : value ]  }
        def regionToKmsKeyIdMap = this.regionToKmsKeyIdMap.collectEntries { key, value -> [ (key.toString()) : value ]  }
        def regionToParameterOverridesMap = this.regionToParameterOverridesMap.collectEntries { key, value -> [ (key.toString()) : value ]  }
        List<String> regions = this.regions*.toString()

        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)

        regions.each { String region ->
            logger.lifecycle("---- Processing region: ${region} ----")

            String s3Bucket
            if (regionToS3BucketMap.containsKey(region)) {
                s3Bucket = regionToS3BucketMap.get(region)
            } else {
                throw new Exception("There was no s3 bucket defined for region: ${region} in regionToS3BucketMap: ${regionToS3BucketMap}")
            }
            String kmsKeyId = regionToKmsKeyIdMap.get(region)

            def processedTemplatePath = helper.uploadArtifactsAndInjectS3UrlsIntoCopiedCFTemplate(region, s3Bucket,
                    s3Prefix, kmsKeyId, forceUploads, templatePath, tokenArtifactMap, project, ant)

            Map<String, String> calculatedParameterOverrides = [:]
            if (! parameterOverrides.isEmpty()) {
                calculatedParameterOverrides = parameterOverrides
            } else if (regionToParameterOverridesMap.containsKey(region)) {
                calculatedParameterOverrides = regionToParameterOverridesMap."${region}"
            }

            helper.deployProcessedTemplate(region, stackName, processedTemplatePath, calculatedParameterOverrides, executeChangeSet, logStackOutputs)
        }
    }

}
