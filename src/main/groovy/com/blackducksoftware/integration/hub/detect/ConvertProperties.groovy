import groovy.json.JsonOutput

def properties = []
String propertyKey = ''
String defaultValue = ''
String description = ''
String propertyType = ''
String group = ''

new File('/Users/erickerwin/Documents/old.properties').eachLine { line ->
    if (line.startsWith('    @ValueDescription')) {
        def descriptionMatch = line =~ /description="([^"]+)"/
        if (descriptionMatch.size() > 0) {
            description = descriptionMatch[0][1]
        }

        def defaultValueMatch = line =~ /defaultValue="([^"]+)"/
        if (defaultValueMatch.size() > 0) {
            defaultValue = defaultValueMatch[0][1]
        }

        def groupMatch = line =~ /group=([^\)]+)\)/
        if (groupMatch.size() > 0) {
            group = groupMatch[0][1]
        }
    } else if (line.startsWith('    @Value')) {
        def propertyKeyMatch = line =~ /@Value\("\$\{([^\}]+)\}/
        if (propertyKeyMatch.size() > 0) {
            propertyKey = propertyKeyMatch[0][1]
        }
    } else if (line.startsWith('    ')) {
        propertyType = line.trim().split(' ')[0]
    } else {
        def propertyMap = [:]
        if (propertyKey) {
            propertyMap.put('propertyKey', propertyKey)
        }
        if (defaultValue) {
            propertyMap.put('defaultValue', defaultValue)
        }
        if (description) {
            propertyMap.put('description', description)
        }
        if (propertyType) {
            propertyMap.put('propertyType', propertyType)
        }
        if (group) {
            propertyMap.put('group', group)
        }
        properties.add(propertyMap)
        propertyKey = ''
        defaultValue = ''
        description = ''
        propertyType = ''
        group = ''
    }
}

String json = JsonOutput.toJson(properties)
println JsonOutput.prettyPrint(json)
