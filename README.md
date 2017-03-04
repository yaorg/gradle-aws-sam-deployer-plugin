Gradle LambdaSam Plugin
=====================

This plugin allows for the convenient deployment of SAM CloudFormation Yaml's from with in a Gradle project.
Underneath the hood this plugin uses the AWS CLI to execute AWS Cloud Formation APIs
 

Usage
-----

To use the plugin, include it as a dependency in your buildscript section of build.gradle:
Please note you also have to have Artifactory defined as a repo in your buildscript section as well.

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

[Current Version](link_to_your_gradle.properties_file_here)

 

Custom tasks
-----------------

The base plugin provides the following custom task types:

**Tasks** | **Description**
---------|-----------------
[deploySAM]()| Deploys a serverless application using a serverless application model yaml file.


Extension properties
--------------------

The plugin defines the following extension properties in the *LambdaSam*
closure:

Inside the `lambdasam` closure is the completely required `config` closure.

**Property name**       | **Type** | **Default value**       | **Description**
------------------------|----------|-------------------------|---
setMsg                  | String   | none                    | prints the message

**Example**

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
apply plugin: 'lambdasam'

lambdasam { 
    config { 
        setMsg "hello from LambdaSam"
    }
}

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
