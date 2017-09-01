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
        println projectDir.canonicalPath

        DetectPropertiesJsonParser detectPropertiesJsonParser = new DetectPropertiesJsonParser()
        Gson gson = new Gson()
        String jsonProperties = new File("${projectDir}/src/main/resources/application_properties.json").text
        detectPropertiesJsonParser.parseJson(gson, jsonProperties)

        final Configuration configuration = new Configuration(Configuration.VERSION_2_3_26)
        configuration.setDirectoryForTemplateLoading(new File("${projectDir}/src/main/resources"))
        configuration.setDefaultEncoding('UTF-8')
        configuration.setLogTemplateExceptions(true)

        final Map model = new HashMap()

        model.put('groups', detectPropertiesJsonParser.groups)
        model.put('detectProperties', detectPropertiesJsonParser.detectProperties)
        model.put('applicationProperties', detectPropertiesJsonParser.applicationProperties)

        final Template detectPropertiesTemplate = configuration.getTemplate('detectProperties.ftl')
        final Template detectConfigurationTemplate = configuration.getTemplate('detectConfiguration.ftl')
        final Template applicationPropertiesTemplate = configuration.getTemplate('applicationProperties.ftl')

        final Writer writer = new OutputStreamWriter(System.out)

        detectPropertiesTemplate.process(model, writer)
        detectConfigurationTemplate.process(model, writer)
        applicationPropertiesTemplate.process(model, writer)
    }
}
