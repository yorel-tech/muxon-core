cd infron-core/integration-tests
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock ../gradlew test --tests "com.onetattva.infron.tests.IntegrationTestSuite"

This will run tests in the specified order:
1. InfraDeployer - starts the infrastructure
2. TenantTests - runs integration tests
3. InfraCleanup - stops the infrastructure

Alternatively, you can run all tests with ordering:
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock ../gradlew test

## pre requisite
./gradlew :services:core-services:bootJar --no-daemon
./gradlew :services:bootstrap-initializer:bootJar --no-daemon
docker build -t infron-core-services:test -f services/core-services/Dockerfile .

### debugging
podman system service --time=0 --log-level debug

### github actions
jobs:
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build core-services image
        run: |
          docker build -t infron-core-services:test -f services/core-services/Dockerfile .

      - name: Run tests
        run: ./gradlew integration-tests:test
