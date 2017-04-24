package com.fieldju.gradle.plugins.lambdasam.services

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.fieldju.commons.StringUtils
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import com.fieldju.gradle.plugins.lambdasam.services.s3.S3Uploader
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class PackageAndDeployTaskHelper {

    Logger logger;

    PackageAndDeployTaskHelper(Logger logger) {
        this.logger = logger
    }

    def multiRegionDeploy(String templatePath,
                          Map<String, String> regionTemplatePathMap,
                          List<String> regions,
                          String stackName,
                          Map<String, Map<String, String>> regionToParameterOverridesMap,
                          boolean executeChangeSet,
                          boolean logStackOutputs) {

        regions.each { String region ->
            Map<String, String> parameterOverrides
            if (regionToParameterOverridesMap.containsKey(region)) {
                parameterOverrides = regionToParameterOverridesMap."${region}"
            } else {
                logger.lifecycle("regionToParameterOverridesMap does not contain an entry for region ${region} defaulting to empty map")
                parameterOverrides = [:]
            }

            String calculatedTemplatePath
            if (StringUtils.isNotBlank(templatePath)) {
                calculatedTemplatePath = templatePath
            } else if (regionTemplatePathMap.containsKey(region)) {
                calculatedTemplatePath = regionTemplatePathMap."${region}"
            } else {
                throw new GradleException("templatePath or regionTemplatePathMap.'\${region}' for " +
                        "region: ${region} must be set")
            }

            CloudFormationDeployer deployer = new CloudFormationDeployer(
                    AmazonCloudFormationClient.builder()
                            .standard()
                            .withRegion(Regions.fromName(region))
                            .build() as AmazonCloudFormationClient
            )

            logger.lifecycle("Creating and executing changeset for ${templatePath} with stackname ${stackName} in region ${region} with overrides ${parameterOverrides}")
            deployer.deployStack(stackName, calculatedTemplatePath, parameterOverrides, executeChangeSet)
            if (logStackOutputs) {
                deployer.logOutputs(stackName)
            }
        }
    }

    def uploadArtifactsAndInjectS3UrlsIntoCopiedCFTemplate(String region,
                                                           String s3Bucket,
                                                           String s3Prefix,
                                                           String kmsKeyId,
                                                           boolean forceUploads,
                                                           String samTemplatePath,
                                                           Map<String, String> tokenArtifactMap,
                                                           Project project,
                                                           AntBuilder ant) {
        Map<String, String> tokenS3UriMap = [:]
        if (tokenArtifactMap.isEmpty()) {
            logger.warn("There were no tokens defined in the tokenArtifactMap, this task will not upload any" +
                    " artifacts to s3 and automatically inject them into the copied deployable sam template.")
        } else {
            S3Uploader s3Uploader = new S3Uploader(region, kmsKeyId, forceUploads)

            tokenArtifactMap.each { token, artifactPath ->
                File artifactToUploadToS3 = new File(artifactPath)
                if (! (artifactToUploadToS3.exists() && artifactToUploadToS3.isFile())) {
                    throw new GradleException("The artifact: ${artifactPath} for token: ${token} did not exist or " +
                            "was not a file (you must archive folders)")
                }

                try {
                    def s3Uri = s3Uploader.uploadWithDedup(s3Bucket, s3Prefix, artifactToUploadToS3)
                    tokenS3UriMap.put(token, s3Uri)
                } catch (Throwable t) {
                    throw new GradleException("Failed to upload artifact ${artifactToUploadToS3.absolutePath}", t)
                }
            }
        }

        logger.lifecycle("Copying sam template to build dir")
        // copy the template to the build dir
        File buildDir = new File("${project.getBuildDir().absolutePath}${File.separator}sam")
        buildDir.mkdirs()
        File dest = new File("${buildDir.absolutePath}${File.separator}sam-deploy-${region}.yaml")
        dest.write(getSamTemplateAsString(samTemplatePath))
        // replace the tokens with the s3 URIs
        tokenS3UriMap.each { token, uri ->
            logger.lifecycle("Injecting ${uri} into ${dest.absolutePath} for token: ${token}")
            ant.replace(file: dest.absolutePath, token: token, value: uri)
        }

        return dest.absolutePath
    }

    private String getSamTemplateAsString(String samTemplatePath) {
        if (samTemplatePath == null || samTemplatePath == "") {
            throw new GradleException("samTemplatePath is a required property")
        }

        File samTemplate = new File(samTemplatePath)

        // if the template is not a real file fail
        if (! (samTemplate.exists() && samTemplate.isFile())) {
            throw new GradleException("The template: ${samTemplatePath} did not exist or was not a file")
        }

        return samTemplate.text
    }

    def deployProcessedTemplate(String region,
                              String stackName,
                              String templatePath,
                              Map<String, String> parameterOverrides,
                              boolean executeChangeSet,
                              boolean logStackOutputs) {

        if (StringUtils.isBlank(stackName)) {
            throw new GradleException("stackName cannot be blank")
        }

        CloudFormationDeployer deployer = new CloudFormationDeployer(
                AmazonCloudFormationClient.builder()
                        .standard()
                        .withRegion(Regions.fromName(region))
                        .build() as AmazonCloudFormationClient
        )

        deployer.deployStack(stackName, templatePath, parameterOverrides, executeChangeSet)
        if (logStackOutputs) {
            deployer.logOutputs(stackName)
        }
    }
}
