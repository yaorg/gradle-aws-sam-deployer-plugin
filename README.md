Gradle Lambda Sam Plugin
=====================

This plugin allows for the convenient deployment of SAM CloudFormation Templates from within a Gradle project.
Underneath the hood this plugin uses the AWS CLI to execute AWS Cloud Formation APIs
 

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

**Property name**  | **Type** | **Description**
-------------------|----------|---
region             | String              | The region to upload the fatJar / lambda code artifact, and execute the cloud formation in
s3Bucket           | String              | The S3 Bucket to store the fatJar / lambda code artifact
s3prefix           | String              | The prefix in the bucket to use when storing the fatJar / lambda code artifact
artifactPath       | String              | The file path to the fatJar / lambda code artifact. ex: `${project.buildDir.absolutePath}${File.seperator}libs${File.seperator}my-lambda-project-fat-jar.jar`
kmsKeyId           | String              | The kms cmk id to use to encrypt the fatJar / lambda code artifact when uploading to S3
samTemplatePath    | String              | The file path to the SAM Yaml or JSON where you have defined your serverless application model
stackName          | String              | The stack name to use for the Cloud Formation stack
parameterOverrides | Map<String, String> | A map of Parameters and there values to supply the Cloud Formation tamplate

**Example**

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
apply plugin: 'lambdasam'

lambdaSam {
    region = 'us-west-2'
    s3Bucket = project.'s3Bucket'
    s3Prefix = project.'s3Prefix'
    stackName = "${project.'env'}-demo-stack"
    samTemplatePath = "${temp.absolutePath}${File.separator}application.yaml"
    artifactPath = "${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar"
}

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
