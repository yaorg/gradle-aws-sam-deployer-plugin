package com.fieldju.gradle.plugins.lambdasam

import org.gradle.api.GradleException

class AwsSamDeployerExtension {

    String region
    String s3Bucket
    String s3Prefix
    String kmsKeyId
    String samTemplatePath
    String stackName
    Map<String, String> tokenArtifactMap = [:]
    Map<String, String> parameterOverrides = [:]
    boolean forceUploads = false

    String getSamTemplatePath() {
        if (samTemplatePath == null || samTemplatePath == "") {
            throw new GradleException("samTemplatePath is a required lambdasam extention property")
        }
        return samTemplatePath
    }

    String getStackName() {
        if (stackName == null || stackName == "") {
            throw new GradleException("${stackName} is a required awssam extention property")
        }
        return stackName
    }
}
