# Deprecated nested workflows

Canonical CI/CD for the monorepo lives at the repository root:

- `.github/workflows/pr-validation.yml` — required PR checks
- `.github/workflows/build-images.yml` — scheduled / manual image builds
- `.github/workflows/appliance-build.yml` — manual appliance builds
- `.github/workflows/integration-tests.yml` — configurable integration tests

These nested workflows are retained temporarily for reference and will be removed once root workflows are verified. Do not add new required branch-protection checks that point here.
