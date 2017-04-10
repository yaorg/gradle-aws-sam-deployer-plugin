package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.services.s3.S3Uploader
import org.gradle.api.GradleException
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

        Map<String, String> tokenArtifactMap = tokenArtifactMap
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
        File dest = new File("${buildDir.absolutePath}${File.separator}sam-deploy.yaml")
        dest.write(getSamTemplateAsString())
        // replace the tokens with the s3 URIs
        tokenS3UriMap.each { token, uri ->
            logger.lifecycle("Injecting ${uri} into ${dest.absolutePath} for token: ${token}")
            ant.replace(file: dest.absolutePath, token: token, value: uri)
        }
    }

    private String getSamTemplateAsString() {
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
}
