#!/bin/bash
echo "Started build"
./gradlew :services:muxon-initializer:bootJar --no-daemon
echo "Built muxon-initializer"
./gradlew :services:core-services:bootJar --no-daemon
echo "Built core-services"
podman build -t muxon-core-services:test -f services/core-services/Dockerfile .
echo "Pushed image muxon-core-services:test to podman"