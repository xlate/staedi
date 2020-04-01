#!/usr/bin/env bash

JAVA_VERSION=$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | awk -F'.' '{print $1"."$2}')

if [[ $(bc -l <<< "${JAVA_VERSION} >= 11") -eq 1 ]]; then
    # Build project, run tests, and generate JavaDocs (Java 11+ only)
    # Maven JavaDoc plugin fails without package goal due to multi-release jar's location of module-info.java
    mvn -B test package javadoc:javadoc
else
    # Build project, run tests
    mvn -B test
fi
