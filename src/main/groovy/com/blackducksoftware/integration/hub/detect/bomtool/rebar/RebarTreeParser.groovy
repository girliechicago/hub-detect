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
package com.blackducksoftware.integration.hub.detect.bomtool.rebar

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import com.blackducksoftware.integration.hub.bdio.simple.DependencyNodeBuilder
import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.ExternalId
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.NameVersionExternalId
import com.blackducksoftware.integration.hub.detect.bomtool.RebarBomTool
import com.moandjiezana.toml.IndentationPolicy

import groovy.transform.TypeChecked

@Component
@TypeChecked
class RebarTreeParser {
    private final Logger logger = LoggerFactory.getLogger(RebarTreeParser.class)

    private final List<String> indentationStrings = [
        '└─ ',
        '   ',
        '├─ ',
        '│  ',
        '└─ '
    ]

    DependencyNode parse(String treeText) {
        DependencyNode root = null
        DependencyNodeBuilder builder = null
        boolean treeStarted = false
        int level = -1
        Stack<DependencyNode> dependencyNodeStack = new Stack()
        for (String line: treeText.split(System.lineSeparator())) {
            if (!line.trim()) {
                continue
            }

            if (!treeStarted && line.startsWith('└─')) {
                treeStarted = true
            }

            if (line.startsWith('===>') || !treeStarted) {
                continue
            }

            RebarPackage rebarPackage = lineToRebarPackage(line)
            DependencyNode dependencyNode = rebarPackageToDependencyNode(rebarPackage)

            if (root == null) {
                root = dependencyNode
                builder = new DependencyNodeBuilder(root)
                dependencyNodeStack.push(dependencyNode)
                continue
            }

            if (rebarPackage.indentation == level) {
                DependencyNode child = dependencyNodeStack.pop()
                builder.addParentNodeWithChildren(dependencyNodeStack.peek(), [child])
            } else if (rebarPackage.indentation < level) {
                level.downto(rebarPackage.indentation) {
                    DependencyNode child = dependencyNodeStack.pop()
                    builder.addParentNodeWithChildren(dependencyNodeStack.peek(), [child])
                }
            }

            dependencyNodeStack.push(dependencyNode)
            level = rebarPackage.indentation
        }

        if (level > 2) {
            level.downto(2) {
                DependencyNode child = dependencyNodeStack.pop()
                builder.addParentNodeWithChildren(dependencyNodeStack.peek(), [child])
            }
        }

        if (builder) {
            builder.root
        } else {
            return null
        }
    }

    // A line might look like this: "   │  └─ bcrypt─0.5.0.3+build.92.refa63df34 (git repo)"
    // name: bcrypt | version: 0.5.0.3 | source: git repo
    private RebarPackage lineToRebarPackage(final String line) {
        final def rebarPackage = new RebarPackage()
        String cleanLine = line
        indentationStrings.each {
            while (cleanLine.contains(it)) {
                rebarPackage.indentation++
                cleanLine = cleanLine.replaceFirst(it, '')
            }
        }
        cleanLine = cleanLine.trim()

        def group = cleanLine =~ /(.*)─(.*?)(\+.*)* \((.*)\)/
        if (group.matches()) {
            rebarPackage.name = group.group(1)
            rebarPackage.version = group.group(2)
            rebarPackage.source = group.group(4)
        } else {
            logger.debug("Line is not parsable: ${cleanLine}")
        }

        rebarPackage
    }

    private DependencyNode rebarPackageToDependencyNode(RebarPackage rebarPackage) {
        ExternalId externalId = new NameVersionExternalId(RebarBomTool.HEX, rebarPackage.name, rebarPackage.version)
        DependencyNode dependencyNode = new DependencyNode(rebarPackage.name, rebarPackage.version, externalId)

        dependencyNode
    }
}