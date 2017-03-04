package com.fieldju.gradle.plugins.lambdasam

import com.amazonaws.services.cloudformation.model.Parameter
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class LambdaSamExtension {
    Logger logger = Logging.getLogger(getClass())

    String region
    String artifactPath
    String s3Bucket
    String s3Prefix
    String kmsKeyId
    String samTemplatePath
    String stackName
    Map<String, String> parameterOverrides

    void setRegion(String region) {
        logger.lifecycle("Setting region to ${region}")
        this.region = region
    }

    File getArtifactToUploadToS3() {
        validateRequiredConfig('artifactPath')
        File artifactToUploadToS3 = new File(getArtifactPath())
        if (! (artifactToUploadToS3.exists() && artifactToUploadToS3.isFile())) {
            throw new GradleException("The template: ${getArtifactPath()} did not exist or was not a file")
        }
        return artifactToUploadToS3
    }

    String getSamTemplateAsString() {
        validateRequiredConfig('samTemplatePath')
        File samTemplate = new File(getSamTemplatePath())

        // if the template is not a real file fail
        if (! (samTemplate.exists() && samTemplate.isFile())) {
            throw new GradleException("The template: ${getSamTemplatePath()} did not exist or was not a file")
        }

        return samTemplate.text
    }

    String getStackName() {
        if (stackName == null || stackName == "") {
            throw new GradleException("${stackName} is a required awssam extention property")
        }
        return stackName
    }

    Set<Parameter> getParameterOverrides() {
        Set<Parameter> parameters = []
        parameterOverrides.each { k,v ->
            parameters.add(
                    new Parameter()
                            .withParameterKey(k)
                            .withParameterValue(v)
                            .withUsePreviousValue(false)
            )
        }
        return parameters
    }

    private void validateRequiredConfig(String key) {
        def val = "get${key.capitalize()}"()
        if (val == null || val == "") {
            throw new GradleException("${key} is a required lambdasam extention property")
        }
    }
}
