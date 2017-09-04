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
        model.put('detectProperties', detectPropertiesJsonParser.detectProperties)
        model.put('applicationProperties', detectPropertiesJsonParser.applicationProperties)

        final Template detectPropertiesTemplate = configuration.getTemplate('detectProperties.ftl')
        final Template detectConfigurationTemplate = configuration.getTemplate('detectConfiguration.ftl')
        final Template applicationPropertiesTemplate = configuration.getTemplate('applicationProperties.ftl')

        String applicationProperitesPath = filePath([projectDir.canonicalPath, 'src', 'main', 'resources', 'application.properties'])
        final Writer writer = new FileWriter(applicationPropertiesPath)

        detectPropertiesTemplate.process(model, writer)
        detectConfigurationTemplate.process(model, writer)
        applicationPropertiesTemplate.process(model, writer)
    }

    private String filePath(List<String> pathPieces) {
        pathPieces.join(File.separator)
    }
}