package com.blackducksoftware.integration.hub.detect

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import com.google.gson.Gson

import freemarker.template.Configuration;
import freemarker.template.Template;

class DetectPropertiesTask extends DefaultTask {
    @TaskAction
    def generate() {
        File projectDir = getProject().getProjectDir()

        DetectPropertiesJsonParser detectPropertiesJsonParser = new DetectPropertiesJsonParser()
        Gson gson = new Gson()
        String jsonFilePath = filePath([projectDir.canonicalPath, 'src', 'main', 'resources', 'application_properties.json'])
        String jsonProperties = new File(jsonFilePath).text
        detectPropertiesJsonParser.parseJson(gson, jsonProperties)

        String resourcesPath = filePath([projectDir.canonicalPath, 'src', 'main', 'resources'])
        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_26)
        configuration.setDirectoryForTemplateLoading(new File(resourcesPath))
        configuration.setDefaultEncoding('UTF-8')
        configuration.setLogTemplateExceptions(true)

        final Map model = new HashMap()
        model.put('groups', detectPropertiesJsonParser.groups)
        model.put('applicationProperties', detectPropertiesJsonParser.applicationProperties)
        model.put('detectProperties', detectPropertiesJsonParser.detectProperties)

        final Template applicationPropertiesTemplate = configuration.getTemplate('applicationProperties.ftl')
        final Template detectPropertiesTemplate = configuration.getTemplate('detectProperties.ftl')
        final Template detectConfigurationTemplate = configuration.getTemplate('detectConfiguration.ftl')

        updateApplicationProperties(projectDir, applicationPropertiesTemplate, model)
        updateDetectProperties(projectDir, detectPropertiesTemplate, model)
        updateDetectConfiguration(projectDir, detectConfigurationTemplate, model)
    }

    private void updateApplicationProperties(File projectDir, Template template, Map model) {
        String applicationPropertiesPath = filePath([projectDir.canonicalPath, 'src', 'main', 'resources', 'application.properties'])
        new File(applicationPropertiesPath).withWriter('UTF-8') {
            template.process(model, it)
        }
    }

    private void updateDetectProperties(File projectDir, Template template, Map model) {
        String detectPropertiesPath = filePath([projectDir.canonicalPath, 'src', 'main', 'groovy', 'com', 'blackducksoftware', 'integration', 'hub', 'detect', 'DetectProperties.groovy'])
        new File(detectPropertiesPath).withWriter('UTF-8') {
            template.process(model, it)
        }
    }

    private void updateDetectConfiguration(File projectDir, Template template, Map model) {
        
    }

        private String filePath(List<String> pathPieces) {
        pathPieces.join(File.separator)
    }
}