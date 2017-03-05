package com.fieldju.gradle.plugins.lambdasam

import com.fieldju.gradle.plugins.lambdasam.tasks.DeploySamTask
import com.fieldju.gradle.plugins.lambdasam.tasks.PackageSamTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class AwsSamDeployerPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = 'aws-sam-deployer'

    Logger logger = Logging.getLogger(getClass())

    @Override
    void apply(Project project) {
        project.extensions.create(EXTENSION_NAME, AwsSamDeployerExtension)
        addTasks(project)
    }

    /**
     * This is where we iterate through the available tasks and add them to the project
     * @param project
     */
    private void addTasks(Project project) {
        TaskDefinitions.values().each { taskDef ->
            project.task(
                    taskDef.name,
                    type: taskDef.taskClass,
                    description: taskDef.description,
                    dependsOn: taskDef.dependsOn
            )
        }
    }

    /**
     * This is where the available tasks for this plugin are defined
     */
    enum TaskDefinitions {
        PACKAGE_SAM_TASK(PackageSamTask, 'packageSam', 'Uploads the the artifacts defined in tokenArtifactMap to S3 and replaces the tokens in the SAM template with the S3 URI.', null),
        DEPLOY_SAM_TASK(DeploySamTask, 'deploySam', 'After uploading the artifacts and prepping the SAM template deploy with use CloudFormation to create or update your stack', 'packageSam')

        final Class taskClass
        final String name
        final String description
        final String dependsOn

        private TaskDefinitions(Class taskClass, String name, String description, String dependsOn) {
            this.taskClass = taskClass
            this.name = name
            this.description = description
            this.dependsOn = dependsOn
        }
    }
}
