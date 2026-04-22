#!/usr/bin/env bash
set -euo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run as root (use: sudo $0)"
  exit 1
fi

USER_NAME="${SUDO_USER:-}"
if [[ -z "${USER_NAME}" ]]; then
  echo "SUDO_USER is empty. Run this via sudo from a normal user."
  exit 1
fi

echo "[docker-install] Installing prerequisites..."
apt-get update
apt-get install -y ca-certificates curl gnupg

echo "[docker-install] Adding Docker apt repo key..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo "[docker-install] Adding Docker apt repository..."
UBUNTU_CODENAME="$(. /etc/os-release && echo "${UBUNTU_CODENAME}")"
ARCH="$(dpkg --print-architecture)"
cat >/etc/apt/sources.list.d/docker.list <<EOF
deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${UBUNTU_CODENAME} stable
EOF

echo "[docker-install] Installing Docker Engine + Compose plugin..."
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "[docker-install] Enabling docker service..."
systemctl enable --now docker

echo "[docker-install] Adding ${USER_NAME} to docker group..."
usermod -aG docker "${USER_NAME}"

echo
echo "[docker-install] Done."
echo "Next:"
echo "  - Log out and log back in (or reboot) so group membership applies."
echo "  - Verify: docker version && docker compose version && docker run --rm hello-world"
