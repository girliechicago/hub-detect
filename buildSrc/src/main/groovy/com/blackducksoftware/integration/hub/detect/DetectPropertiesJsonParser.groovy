package com.blackducksoftware.integration.hub.detect

import java.lang.reflect.Type

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DetectPropertiesJsonParser {
    List<Map<String, String>> groups = []
    List<Map<String, String>> detectProperties = []

    void parseJson(Gson gson, String json) {
        Type detectPropertiesListType = new TypeToken<List<DetectPropertyData>>(){}.getType();
        List<DetectPropertyData> detectPropertyDataList = gson.fromJson(json, detectPropertiesListType)

        detectPropertyDataList.each {
            if (it.group) {
                String groupText = it.group.toLowerCase().replaceAll('_', ' ')
                groups.add([javaName: it.group, textName: groupText])
            }

            def keyPieces = it.propertyKey.split('\\.')
            def javaName = keyPieces[1] + keyPieces[2..-1].collect {
                it[0].toUpperCase() + it[1..-1]
            }.join('')

            def detectProperty = [key: it.propertyKey, javaName: javaName]
            if (it.description)
                detectProperty.put('description', it.description)
            if (it.defaultValue)
                detectProperty.put('defaultValue', it.defaultValue)
            if (it.propertyType)
                detectProperty.put('type', it.propertyType)
            if (it.group)
                detectProperty.put('group', it.group)

            detectProperties.add(detectProperty)
        }
    }
}
