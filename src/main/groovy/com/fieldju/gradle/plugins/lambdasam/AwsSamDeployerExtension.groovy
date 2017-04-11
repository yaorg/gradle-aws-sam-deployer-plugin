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


    @Override
    public String toString() {
        return "AwsSamDeployerExtension{" +
                "region='" + region + '\'' +
                ", s3Bucket='" + s3Bucket + '\'' +
                ", s3Prefix='" + s3Prefix + '\'' +
                ", kmsKeyId='" + kmsKeyId + '\'' +
                ", samTemplatePath='" + samTemplatePath + '\'' +
                ", stackName='" + stackName + '\'' +
                ", tokenArtifactMap=" + tokenArtifactMap +
                ", parameterOverrides=" + parameterOverrides +
                ", forceUploads=" + forceUploads +
                '}';
    }
}
