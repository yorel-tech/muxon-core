### docker example
#### docker-compose.yml
services:
core-services:
image: infron/core-services:latest
volumes:
- ./initial-config.yaml:/etc/infron/initial-config.yaml:ro
environment:
INITIAL_CONFIG_PATH: /etc/infron/initial-config.yaml

### Kubernetes example
apiVersion: v1
kind: ConfigMap
metadata:
name: infron-initial-config
data:
initial-config.yaml: |
provider:
protocol: OIDC
metadata:
issuer: "https://idp.example"
clientId: "infron"
clientSecretRef: "secret://k8s/idp-secret#client_secret"

---

apiVersion: apps/v1
kind: Deployment
spec:
template:
spec:
containers:
- name: core-services
image: infron/core-services
volumeMounts:
- name: initial-config
mountPath: /etc/infron/
volumes:
- name: initial-config
configMap:
name: infron-initial-config
items:
- key: initial-config.yaml
path: initial-config.yaml
# mount secrets separately
