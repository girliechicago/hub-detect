/*
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect.bomtool.pear

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.bdio.simple.model.Forge
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId
import com.blackducksoftware.integration.hub.detect.DetectConfiguration
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableOutput

import groovy.transform.TypeChecked

@Component
@TypeChecked
class PearDependencyFinder {
    private static final String EQUALS_LINE = '==='
    private static final String DEPENDENCY_TYPE_PACKAGE = 'package'

    private final Logger logger = LoggerFactory.getLogger(PearDependencyFinder.class)

    @Autowired
    DetectConfiguration detectConfiguration

    public Set<DependencyNode> parsePearDependencyList(ExecutableOutput pearListing, ExecutableOutput pearDependencies) {
        Set<DependencyNode> childNodes = []

        if (pearDependencies.errorOutput || pearListing.errorOutput) {
            logger.error("There was an error during execution.${System.lineSeparator()}Pear dependency list error: ${pearListing.errorOutput}${System.lineSeparator()}Pear installed dependencies list error: ${pearDependencies.errorOutput}")
            if (!pearDependencies.standardOutput && !pearListing.standardOutput) {
                return childNodes
            }
        } else if (!pearDependencies.standardOutput || !pearListing.standardOutput) {
            logger.error("Not enough information retrieved from running pear commands")
            return childNodes
        }

        def nameList = findDependencyNames(pearDependencies.standardOutput)
        childNodes = createPearDependencyNodeFromList(pearListing.standardOutput, nameList)

        childNodes
    }

    private Set<String> findDependencyNames(String list) {
        Map<String, String> cleanDependencyList = cleanDependencyList(list, false)
        if (!detectConfiguration.pearNotRequiredDependencies) {
            def iterator = cleanDependencyList.entrySet().iterator()

            while (iterator.hasNext()) {
                if (iterator.next().value.toLowerCase().equals('no')) {
                    iterator.remove()
                }
            }
        }
        return cleanDependencyList.keySet()
    }

    private Set<DependencyNode> createPearDependencyNodeFromList(String list, Set<String> dependencyNames) {
        Set<DependencyNode> dependencyNodes = []
        Map<String, String> installedDependencies = cleanDependencyList(list, true)

        dependencyNames.each { String dependencyKey ->
            if (installedDependencies.containsKey(dependencyKey)) {
                dependencyNodes.add(new DependencyNode(dependencyKey, installedDependencies.getAt(dependencyKey), new NameVersionExternalId(Forge.PEAR, dependencyKey, installedDependencies.getAt(dependencyKey))))
            }
        }

        dependencyNodes
    }

    private Map<String, String> cleanDependencyList(String list, boolean isCheckInstalledDependencies) {
        def dependencies = [:]
        def isSkipNextLine = true
        String[] eachLine = list.split(System.lineSeparator())
        eachLine.each { String line ->
            if (line.contains(EQUALS_LINE) || line.empty) {
                isSkipNextLine = true
            } else if (isSkipNextLine) {
                isSkipNextLine = false
            } else {
                String[] lineSections = line.split(' ')
                def lineSectionsList = lineSections.toList()
                lineSectionsList.removeAll('')
                Map.Entry<String, String> dependency = getContentFromLine(lineSectionsList, isCheckInstalledDependencies)
                if (dependency != null && !dependencies.containsKey(dependency.key)) {
                    dependencies.put(dependency.key, dependency.value)
                }
            }
        }

        dependencies
    }

    private Map.Entry<String, String> getContentFromLine(List<String> lineSections, boolean isCheckInstalledDependencies) {
        Map.Entry<String, String> mapEntry

        if (isCheckInstalledDependencies) {
            mapEntry = new AbstractMap.SimpleEntry<String, String>(lineSections.get(0), lineSections.get(1))
        } else {
            if (lineSections.get(1).toLowerCase().equals(DEPENDENCY_TYPE_PACKAGE)) {
                String messyDependencyName = lineSections.get(2)
                int slashIndex = messyDependencyName.indexOf('/') + 1
                String dependencyName = messyDependencyName.substring(slashIndex)
                mapEntry = new AbstractMap.SimpleEntry<String, String>(dependencyName, lineSections.get(0))
            }
        }

        mapEntry
    }
}
