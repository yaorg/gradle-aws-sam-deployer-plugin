package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.fieldju.gradle.plugins.lambdasam.LambdaSamExtension
import com.fieldju.gradle.plugins.lambdasam.LambdaSamPlugin
import groovy.json.JsonBuilder
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.com.google.common.io.Files

class PackageSamTask extends SamTask {
    static final String TASK_GROUP = 'LambdaSam'
    static final String CODE_URI_TOKEN = '@@CODE_URI@@'

    PackageSamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @TaskAction
    void taskAction() {
        def config = project.extensions.getByName(LambdaSamPlugin.EXTENSION_NAME) as LambdaSamExtension

        File artifact = config.getArtifactToUploadToS3()
        def s3Bucket = config.getS3Bucket()
        def s3Prefix = config.getS3Prefix()
        def kmsKeyId = config.getKmsKeyId()
        def artifactName = "${project.getName()}-${UUID.randomUUID().toString()}.jar"
        def key = "$s3Prefix/$artifactName"
        def s3Uri = "s3://${s3Prefix}/${key}"

        AmazonS3 s3
        if (kmsKeyId == null || kmsKeyId == "") {
            s3 = AmazonS3Client.builder().standard().withRegion(Regions.fromName(config.getRegion())).build()
        } else {
            throw new GradleException("KMS encryption of artifact not implemented yet!")
        }

        logger.lifecycle("Uploading ${artifact.absolutePath} to ${s3Uri}")

        try {
            s3.putObject(s3Bucket, key, artifact)
        } catch (Throwable t) {
            throw new GradleException("Failed to upload artifact ${artifact.absolutePath} to ${s3Uri}", t)
        }

        logger.lifecycle("Successfully uploaded ${artifact.absolutePath} to ${s3Uri}")

        logger.lifecycle("Copying sam template to build dir")
        // copy the template to the build dir
        File buildDir = new File("${project.getBuildDir().absolutePath}${File.separator}sam")
        buildDir.mkdirs()
        File dest = new File("${buildDir.absolutePath}${File.separator}sam-deploy.yaml")
        dest.write(config.getSamTemplateAsString())
        // replace the code uri token with the s3 uri
        ant.replace(file: dest.absolutePath, token: CODE_URI_TOKEN, value: s3Uri)

        logger.lifecycle("Successfully injected s3Uri into ${dest.absolutePath}, ready to deploy")
    }
}
