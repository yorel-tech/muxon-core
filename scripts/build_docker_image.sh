#!/bin/bash
echo "Started build"
./gradlew :services:muxon-initializer:bootJar --no-daemon
echo "Built muxon-initializer"
./gradlew :services:core-services:bootJar --no-daemon
echo "Built core-services"
podman build -t infron-core-services:test -f services/core-services/Dockerfile .
echo "Pushed image infron-core-services:test to podman"