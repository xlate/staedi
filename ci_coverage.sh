#!/usr/bin/env bash

if [[ $(bc -l <<< "$(java -version 2>&1 | awk -F '\"' '/version/ {print $2}' | awk -F'.' '{print $1"."$2}') >= 11") -eq 1 ]]; then
    mvn -Pcoverage -B verify sonar:sonar -Dsonar.projectKey=xlate_staedi
else
    echo "Not Java 11"
fi
