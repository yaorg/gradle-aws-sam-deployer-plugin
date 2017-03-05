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
    Map<String, String> parameterOverrides
    boolean forceUploads = false

    String getSamTemplateAsString() {
        if (samTemplatePath == null || samTemplatePath == "") {
            throw new GradleException("${samTemplatePath} is a required lambdasam extention property")
        }

        File samTemplate = new File(samTemplatePath)

        // if the template is not a real file fail
        if (! (samTemplate.exists() && samTemplate.isFile())) {
            throw new GradleException("The template: ${samTemplatePath} did not exist or was not a file")
        }

        return samTemplate.text
    }

    String getStackName() {
        if (stackName == null || stackName == "") {
            throw new GradleException("${stackName} is a required awssam extention property")
        }
        return stackName
    }
}
