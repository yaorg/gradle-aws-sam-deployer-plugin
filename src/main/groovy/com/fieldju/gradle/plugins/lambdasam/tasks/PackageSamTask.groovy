package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerExtension
import com.fieldju.gradle.plugins.lambdasam.AwsSamDeployerPlugin
import com.fieldju.gradle.plugins.lambdasam.services.s3.S3Uploader
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class PackageSamTask extends SamTask {
    static final String TASK_GROUP = 'AWS Lambda SAM Deployer'

    PackageSamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @TaskAction
    void taskAction() {
        def config = project.extensions.getByName(AwsSamDeployerPlugin.EXTENSION_NAME) as AwsSamDeployerExtension
        logExtraDetails(config)

        def s3Bucket = config.getS3Bucket()
        def s3Prefix = config.getS3Prefix()

        S3Uploader s3Uploader = new S3Uploader(config.getRegion(), config.getKmsKeyId(), config.getForceUploads())

        Map<String, String> tokenArtifactMap = config.getTokenArtifactMap()
        Map<String, String> tokenS3UriMap = [:]
        if (tokenArtifactMap.isEmpty()) {
            logger.warn("There were no tokens defined in the tokenArtifactMap, this task will not upload any" +
                    " artifacts to s3 and automatically inject them into the copied deployable sam template.")
        } else {
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
        File dest = new File("${buildDir.absolutePath}${File.separator}sam-deploy.yaml")
        dest.write(config.getSamTemplateAsString())
        // replace the tokens with the s3 URIs
        tokenS3UriMap.each { token, uri ->
            logger.lifecycle("Injecting ${uri} into ${dest.absolutePath} for token: ${token}")
            ant.replace(file: dest.absolutePath, token: token, value: uri)
        }
    }
}
