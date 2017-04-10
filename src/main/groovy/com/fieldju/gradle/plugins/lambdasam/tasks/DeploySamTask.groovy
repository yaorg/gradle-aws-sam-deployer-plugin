package com.fieldju.gradle.plugins.lambdasam.tasks

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.Parameter
import com.amazonaws.services.cloudformation.model.TemplateParameter
import com.fieldju.gradle.plugins.lambdasam.services.cloudformation.CloudFormationDeployer
import org.gradle.api.tasks.Input

class DeploySamTask extends SamTask {

    @Input
    String stackName

    @Input
    Map<String, String> parameterOverrides

    DeploySamTask() {
        group = TASK_GROUP
    }

    /**
     * This is the entry point for this task
     */
    @Override
    void taskAction() {
        CloudFormationDeployer deployer = new CloudFormationDeployer(
                AmazonCloudFormationClient.builder()
                        .standard()
                        .withRegion(Regions.fromName(region))
                        .build() as AmazonCloudFormationClient
        )

        String samTemplate = new File("${project.buildDir.absolutePath}${File.separator}sam${File.separator}sam-deploy.yaml").text

        List<TemplateParameter> templateDefinedParameters = deployer.getTemplateParameters(samTemplate)
        List<Parameter> parameterOverrides = mergeParameters(parameterOverrides, templateDefinedParameters)
        Set<String> capabilities = ['CAPABILITY_IAM'] as Set

        def changeSetMetadata = deployer.createAndWaitForChangeSet(stackName, samTemplate, parameterOverrides, capabilities)

        def executeChangeSet = true
        if (executeChangeSet) {
            deployer.executeChangeSet(changeSetMetadata.name, stackName)
            deployer.waitForExecute(changeSetMetadata.type, stackName)

            logger.lifecycle("Successfully executed change set ${changeSetMetadata.name} for stack name: ${stackName}")
        } else {
            deployer.logChangeSetDescription(changeSetMetadata.name, stackName)
        }
    }

    /**
     * CloudFormation create change set requires a value for every parameter from the template, either specifying a
     * new value or use previous value. For convenience, this method will accept new parameter values and generates
     * a collection of all parameters in a format that ChangeSet API  will accept
     *
     * @param parameterOverrides
     * @param templateDefinedParameters
     * @return
     */
    private static List<Parameter> mergeParameters(Map<String, String> parameterOverrides,
                                                   List<TemplateParameter> templateDefinedParameters) {

        List<Parameter> parameters = []

        templateDefinedParameters.each { templateDefinedParameter ->
            def key = templateDefinedParameter.parameterKey
            if (! parameterOverrides.containsKey(key) && templateDefinedParameter.getDefaultValue()) {
                // Parameters that have default value and not overridden, should not be
                // passed to CloudFormation
                return
            }

            Parameter parameter = new Parameter()
            if (parameterOverrides.containsKey(key)) {
                parameter.withParameterKey(key).withParameterValue(parameterOverrides.get(key))
            } else {
                parameter.withUsePreviousValue(true)
            }
            parameters.add(parameter)
        }

        return parameters
    }
}
