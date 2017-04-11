package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.services.PackageAndDeployTaskHelper
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class PackageSamTask extends SamTask {

    @Input
    @Optional
    String s3Bucket

    @Input
    @Optional
    String s3Prefix

    @Input
    @Optional
    String kmsKeyId

    @Input
    @Optional
    boolean forceUploads

    @Input
    String samTemplatePath

    @Input
    Map<String, String> tokenArtifactMap

    PackageSamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @Override
    void taskAction() {
        PackageAndDeployTaskHelper helper = new PackageAndDeployTaskHelper(logger)
        helper.uploadArtifactsAndInjectS3UrlsIntoCopiedCFTemplate(region, s3Bucket, s3Prefix, kmsKeyId, forceUploads,
                samTemplatePath, tokenArtifactMap, project, ant)
    }
}
