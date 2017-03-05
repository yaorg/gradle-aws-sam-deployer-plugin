Gradle Lambda Sam Plugin
=====================

This plugin allows for the convenient deployment of SAM CloudFormation Templates from within a Gradle project.
Underneath the hood this plugin uses the AWS CLI to execute AWS Cloud Formation APIs

This plugin is inspired by the offical AWS CLI, specifically the [package](https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/package.py) and [deploy](https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/deploy.py) commands.

I kept finding my self executing shell in my gradle scripts and was motivated to create this plugin.

Usage
-----

To use the plugin, include it as a dependency in your buildscript section of build.gradle:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
buildscript { 
    repositories { 
        jcenter() 
        maven { 
            url "https://dl.bintray.com/fieldju/maven"
        }
    }

    dependencies { 
        classpath(group: 'com.fiedldju:gradle-lambdasam-plugin:[ENTER VERSION HERE]') 
    } 
}
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

[Current Version](https://github.com/fieldju/jvm-lambda-template/releases)

 
Tasks
-----------------

The lambda sam plugin provides the following Tasks:

**Tasks** | **Description** | **DependsOn**
----------|-----------------|--------------
[packageSam](src/main/groovy/com/fieldju/gradle/plugins/lambdasam/tasks/PackageSam.groovy)| Uploads the fatJar / artifact to S3 and injects the code uri into the SAM Template replacing @@CODE_URI@@ tokens. [See the integration test for example](src/integration-test/resources/application.yaml#L14) | N/A
[deploySam](src/main/groovy/com/fieldju/gradle/plugins/lambdasam/tasks/DeploySamTask.groovy)| Deploys a serverless application using a serverless application model yaml file.                                | packageSam


Extension properties
--------------------

The plugin defines the following extension properties in the *lambdasam*
closure:

**Property name**  | **Type**            | **Required** | **Description**
-------------------|---------------------|--------------|---
region             | String              | Yes          | The region to upload the fatJar / lambda code artifact, and execute the cloud formation in
s3Bucket           | String              | Yes          | The S3 Bucket to store the fatJar / lambda code artifact
s3prefix           | String              | No           | The prefix in the bucket to use when storing the fatJar / lambda code artifact
kmsKeyId           | String              | No           | The kms cmk id to use to encrypt the fatJar / lambda code artifact when uploading to S3, if not supplied server side AES256 will be used
samTemplatePath    | String              | Yes          | The file path to the SAM Yaml or JSON where you have defined your serverless application model
stackName          | String              | Yes          | The stack name to use for the Cloud Formation stack
parameterOverrides | Map<String, String> | No           | A map of Parameters and there values to supply the Cloud Formation template
tokenArtifactMap   | Map<String, String> | No           | A map of ant style tokens ex: @@FAT_JAR_URI@@ to file paths ex: `${project.buildDir.absolutePath}${File.seperator}libs${File.seperator}my-lambda-project-fat-jar.jar`, the package command uses this map to upload the files to s3 and replaces the tokens in the sam template with the S3 URIs
forceUploads       | boolean             | No           | By default if this is left off or set to false, to package command uses m5 hashes to skip files that have not changed since the last deploy. Set this to true to force re-uploading

**Example**

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
apply plugin: 'lambdasam'

lambdaSam {
    region = getRequiredTestParam('REGION', 'The region to use for S3, KMS, and CloudFormation')
    s3Bucket = getRequiredTestParam('S3_BUCKET', 'The s3 bucket to upload the lambda fat jar')
    s3Prefix = getRequiredTestParam('S3_PREFIX', 'The prefix / folder to store the fat jar in')
    stackName = testStackName
    samTemplatePath = "${temp.absolutePath}${File.separator}application.yaml"
    tokenArtifactMap = [
            '@@LAMBDA_FAT_JAR@@': "${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar"
    ]
    forceUploads = true
}

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
