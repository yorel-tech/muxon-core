**Ubuntu Setup**

install -
git

ssh keys
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAH9YTCz3PSRzMldPyTXjLqiS5V6CWyqShXe/0wAGthu chaitanya118@gmail.com
cat ~/.ssh/id_ed25519.pub

git clone git@github.com:onetattva/infron-core.git

**install podman
flatpak
sudo apt update
sudo apt install flatpak
flatpak remote-add --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
flatpak install flathub io.podman_desktop.PodmanDesktop
sudo apt-get -y install podman podman-compose

Edit /etc/containers/registries.conf
[registries.search]
unqualified-search-registries = ["docker.io", "registry.access.redhat.com", "registry.fedoraproject.org"]

podman-compose -f compose/dev-stack.yml up -d

Any issues with podman desktop
Run:
# start user socket
systemctl --user enable --now podman.socket
And
restatr podman desktop


Intellij:
1. Select temurin jdk to download
2. Gets downloaded at /home/krishnac/.jdks/temurin-25.0.1
3. Import existing project
2. chmod +x gradlew
3. Add following to ~/.profile
\# env vars
export JAVA_HOME=/home/krishnac/.jdks/temurin-25.0.1
export PATH="$JAVA_HOME/bin:$PATH"
4. 
Web:
sudo apt install npm
update node:
sudo apt remove nodejs -y
curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
npm install --save-dev @types/node@latest  

keycloak web setup 
well known end point: http://127.0.0.1:8085/realms/infron-dev/.well-known/openid-configuration
Error: No authority or metadataUrl configured on settings
- create .env.local in /web folder and add below content
  NEXT_PUBLIC_OIDC_AUTHORITY=http://localhost:8085/realms/infron-dev
  NEXT_PUBLIC_OIDC_CLIENT_ID=infron-web
  NEXT_PUBLIC_OIDC_REDIRECT_URI=http://localhost:4000/auth/callback
  NEXT_PUBLIC_OIDC_POST_LOGOUT_REDIRECT_URI=http://localhost:4000/
  NEXT_PUBLIC_OIDC_SCOPE=openid profile email
  NEXT_PUBLIC_API_BASE=http://localhost:8080

Error:
2025-11-17 17:27:07 DEBUG o.s.s.o.s.r.a.JwtAuthenticationProvider - Failed to authenticate since the JWT was invalid
2025-11-17 17:27:07 DEBUG o.s.s.authentication.ProviderManager - Authentication failed with provider JwtAuthenticationProvider since An error occurred while attempting to decode the Jwt: Missing required audience 'infron-api'

- Add an Audience mapper via a Client Scope (UI steps)
- In the Keycloak Admin Console go to your Realm → Client Scopes (left nav).
- Click Create client scope.
- Name: aud-infron-api (or any name)
- Protocol: openid-connect
- Save.

- Open the newly created client scope → go to the Mappers tab → Create (or Add mapper).
- Mapper type is audience. Create an Audience mapper (preferred):
- Name: aud-infron-api
- Set Included Client Audience: infron-api
- Ensure Add to access token is ON.

- Assign the client scope to your UI client:
- Open Clients → select your UI client. (infron-web)
- Go to Client Scopes tab.
- Under Default Client Scopes (or Optional), add aud-infron-api. Usually add under Default so every token includes it.
- Re-login in UI (obtain a fresh token) and verify aud contains infron-api.

* Postgres Client

- DBeaver:
  sudo snap install dbeaver-ce
- psql
  sudo apt install postgresql-client
- command to connect
  psql --host=localhost --port=5432 -U infron

* Openapi
- install redocly
  npm i @redocly/cli@latest
  npx @redocly/cli lint openapi/openapi.yaml