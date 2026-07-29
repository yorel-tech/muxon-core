### Initial configuration ###

#### local dev
- run the script using initial-config.yaml and passkey
- use the generated application.yaml to start the service

#### docker/podman

- save passkey in docker/podman
- run the script using initial-config.yaml and passkey.
- script will be part of the image and will run only when application.yaml is missing
- application.yaml is generated inside the container.

#### kubernetes
- todo

#### keycloak configuration
To list users, you need the following config on keycloak
1. Add view-users, view-groups roles to service account for the client: muxon-api in this case
2. Add client seccret in .env file or -D
3. use vault for final