package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.services.PackageAndDeployTaskHelper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class MultiRegionPackageAndDeploySamTask extends DefaultTask {

    @Input
    def regions = []

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

    @TaskAction
    void taskAction() {
        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)

        regions.each { String region ->
            logger.lifecycle("---- Processing region: ${region} ----")

            String s3Bucket = regionToS3BucketMap."${region}"
            String kmsKeyId = regionToKmsKeyIdMap."${region}"

            def processedTemplatePath = helper.uploadArtifactsAndInjectS3UrlsIntoCopiedCFTemplate(region, s3Bucket,
                    s3Prefix, kmsKeyId, forceUploads, templatePath, tokenArtifactMap, project, ant)

            Map<String, String> calculatedParameterOverrides = [:]
            if (parameterOverrides.isEmpty()) {
                calculatedParameterOverrides = parameterOverrides
            } else if (regionToParameterOverridesMap.containsKey(region)) {
                calculatedParameterOverrides = regionToParameterOverridesMap."${region}"
            }

            helper.deployProcessedTemplate(region, stackName, processedTemplatePath, calculatedParameterOverrides, executeChangeSet)
        }
    }

}
