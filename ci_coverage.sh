#!/usr/bin/env bash

JAVA_VERSION=$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | awk -F'.' '{print $1"."$2}')

if [[ $(bc -l <<< "${JAVA_VERSION} >= 11") -eq 1 ]]; then
    # export BRANCH=$(if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then echo $TRAVIS_BRANCH; else echo "pull-request-$TRAVIS_PULL_REQUEST"; fi)

    if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
        mvn -Pcoverage -B verify sonar:sonar -Dsonar.projectKey=xlate_staedi
    else
        # Skip Sonar analysis for PRs (not supported)
        mvn -Pcoverage -B verify
    fi
else
    echo "Skipping coverage analysis for JAVA_VERSION: ${JAVA_VERSION}"
fi
