# Gradle Lambda Sam Plugin

[![][travis img]][travis]
[![][license img]][license]

This plugin allows for the convenient deployment of Serverless Application Model (SAM) CloudFormation Templates for lambda 
based serverless applications from within a Gradle project.

This plugin is inspired by the official AWS CLI, specifically the
`package` ([code](https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/package.py) / 
[description](https://github.com/aws/aws-cli/blob/1.11.56/awscli/examples/cloudformation/_package_description.rst)) 
and `deploy` ([code](https://github.com/aws/aws-cli/blob/1.11.56/awscli/customizations/cloudformation/deploy.py) /
 [description](https://github.com/aws/aws-cli/blob/1.11.56/awscli/examples/cloudformation/_deploy_description.rst)) commands which are described in AWS's [Introducing Simplified Serverless Application Deployment and Management](https://aws.amazon.com/blogs/compute/introducing-simplified-serverless-application-deplyoment-and-management/) blog post.

I kept finding myself executing shell to use the CLI to execute those commands in a non-elegant manner in my gradle scripts and was motivated to create this plugin.

This plugin has 2 tasks packageSam and deploySam described below that closely mirror the AWS CLI Commands.

## Tasks

### [packageSam](src/main/groovy/com/fieldju/gradle/plugins/lambdasam/tasks/PackageSamTask.groovy)

This task uses the `tokenArtifactMap`, `region`, `s3Bucket`, `s3Prefix`, `kmsKeyId`, `forceUploads` extension properties to upload artifacts defined in the map to s3. 

If you supply a KMS CMK id via the `kmsKeyId` property the uploads will be configured to be encrypted with your KMS key. If you do not supply a KMS CMK id the task will configure your uploads to use the server side AES256 encryption.

This task uses MD5 hashes to name your files, so that it can detect if no changes have been made since the last upload and skip re-uploading files that have not changed. You can use `forceUploads = true` to always force fresh uploads.

Once uploads are complete this tasks copies your SAM Template that you defined in `samTemplatePath` to the build dir in the `sam` folders and uses ant to replace the tokens you defined in the map with the S3 URIs so that when you deploy the template CloudFormation will know where your artifacts are in S3. 

### [deploySam](src/main/groovy/com/fieldju/gradle/plugins/lambdasam/tasks/DeploySamTask.groovy)

This task uses the template outputted by the `packageSam` task and CloudFormation to create a change set to either create or update your stack using the `stackName` property you configured.

This task will ask CloudFormation to validate your template and get all the parameters that the template requires. 
The plugin then iterates over the template parameters and either uses your overrides defined in `parameterOverrides` or tells CloudFormation to use the previous value.

## Usage

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

Configure the extension properties in the *lambdasam* closure:

**Property name**  | **Type**            | **Required** | **Description**
-------------------|---------------------|--------------|---
region             | String              | Yes          | The region to upload the fatJar / lambda code artifact, and execute the cloud formation in
s3Bucket           | String              | Yes          | The S3 Bucket to store the fatJar / lambda code artifact
s3prefix           | String              | No           | The prefix in the bucket to use when storing the fatJar / lambda code artifact
kmsKeyId           | String              | No           | The kms cmk id to use to encrypt the fatJar / lambda code artifact when uploading to S3, if not supplied server side AES256 will be used
samTemplatePath    | String              | Yes          | The file path to the SAM Yaml or JSON where you have defined your serverless application model
stackName          | String              | Yes          | The stack name to use for the Cloud Formation stack
parameterOverrides | Map<String, String> | No           | A map of Parameters and there values to supply the Cloud Formation template
tokenArtifactMap   | Map<String, String> | No           | A map of ant style tokens ex: `@@FAT_JAR_URI@@` to file paths ex: `${project.buildDir.absolutePath}${File.seperator}libs${File.seperator}my-lambda-project-fat-jar.jar`, the package command uses this map to upload the files to s3 and replaces the tokens in the sam template with the S3 URIs
forceUploads       | boolean             | No           | By default if this is left off or set to false, to package command uses m5 hashes to skip files that have not changed since the last deploy. Set this to true to force re-uploading

**Example**

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

apply plugin: 'lambdasam'

lambdaSam {
    region = getRequiredTestParam('REGION', 'The region to use for S3, KMS, and CloudFormation')
    s3Bucket = getRequiredTestParam('S3_BUCKET', 'The s3 bucket to upload the lambda fat jar')
    s3Prefix = getRequiredTestParam('S3_PREFIX', 'The prefix / folder to store the fat jar in')
    stackName = "demo-hello-word-jvm-lamdda"
    samTemplatePath = "${temp.absolutePath}${File.separator}application.yaml"
    tokenArtifactMap = [
            '@@LAMBDA_FAT_JAR@@': "${temp.absolutePath}${File.separator}jvm-hello-world-lambda.jar"
    ]
    parameterOverrides = [
            Foo: 'bar'
    ]
    forceUploads = true
}

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

execute the deploySam command `./gradlew clean build deploySam`

## License

The Gradle Lambda Sam Plugin is released under the [Apache License, Version 2.0](LICENSE.txt)

[travis]:https://travis-ci.org/fieldju/gradle-lambdasam-plugin
[travis img]:https://api.travis-ci.org/fieldju/gradle-lambdasam-plugin.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg
