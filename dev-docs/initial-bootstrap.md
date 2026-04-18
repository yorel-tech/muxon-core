# Bootstrap initializer (developer notes)

The **muxon-initializer** JAR runs before **core-services** to:

- Read **`initial-config.yaml`**
- Read **secret files** (passphrase, OIDC client secret, DB password)
- Run **Flyway** migrations
- Seed initial IdP / tenant data
- Write generated Spring configuration under **`/etc/infron/`** (merged at runtime via `--spring.config.additional-location=optional:file:/etc/infron/`)

## Admin installation guides

End-to-end installation (Docker, Kubernetes/Helm, appliance, cloud-init) is documented for operators in:

- [docs/admin-guide/install-core.md](../docs/admin-guide/install-core.md)
- [docs/admin-guide/install-nexus.md](../docs/admin-guide/install-nexus.md)

## Docker (compose)

Use [compose/infron.yml](../compose/infron.yml) with **secrets** and **configs** (not raw `environment:` for passwords).

## Kubernetes

The **infron-core** Helm chart renders a **ConfigMap** for `initial-config.yaml` and runs bootstrap in an **init container**. Pass a full file with:

```bash
helm install infron ./deploy/helm/infron-core -n infron \
  --set-file bootstrap.initialConfigYaml=./initial-config.yaml
```

For the **infron-nexus** parent chart, use:

```bash
--set-file infron-core.bootstrap.initialConfigYaml=./initial-config.yaml
```
