package com.fieldju.gradle.plugins.lambdasam.tasks

import com.fieldju.gradle.plugins.lambdasam.LambdaSamPlugin
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

@Slf4j
class DeploySamTaskIntegrationTest {

    @Test
    void "test that the package and deploy sam tasks can deploy hello word lambda function"() {
        File temp = File.createTempDir()
        def project = ProjectBuilder.builder().withName('DeploySamTaskIntegrationTest').withProjectDir(temp).build()
        LambdaSamPlugin plugin = new LambdaSamPlugin()
        plugin.apply(project)
        (project.'lambdaSam') {
            region = 'us-west-2'
            s3Bucket = ""
            s3Prefix = ''
            samTemplatePath = "${project.projectDir.absolutePath}${File.separator}application.yaml"
            stackName = ''
            parameterOverrides = [
                    Environment: 'test',
            ]
            capabilities = [
                    'CAPABILITY_IAM'
            ] as Set
        }
        def deploySamTask = project.tasks.getByName(LambdaSamPlugin.TaskDefinitions.DEPLOY_SAM_TASK.name as String) as DeploySamTask
        deploySamTask.taskAction()
    }

}
