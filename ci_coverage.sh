#!/usr/bin/env bash

if [[ $(bc -l <<< "$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | awk -F'.' '{print $1"."$2}') >= 11") -eq 1 ]]; then
    export BRANCH=$(if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then echo $TRAVIS_BRANCH; else echo "pull-request-$TRAVIS_PULL_REQUEST"; fi)
    mvn -Pcoverage -B verify sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=xlate -Dsonar.projectKey=xlate_staedi -Dsonar.login=$SONAR_LOGIN -Dsonar.branch.name=$BRANCH
else
    echo "Not Java 11"
fi
