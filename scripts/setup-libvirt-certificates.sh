#!/bin/bash

# Fix for Libvirt CA certificate issue
# This script creates the necessary certificate structure for libvirt TLS connections

echo "Setting up Libvirt TLS certificates..."

# Create CA directory structure
sudo mkdir -p /etc/pki/CA
sudo mkdir -p /etc/pki/libvirt
sudo mkdir -p /etc/pki/qemu

# Create a self-signed CA certificate if it doesn't exist
if [ ! -f /etc/pki/CA/cacert.pem ]; then
    echo "Creating self-signed CA certificate..."
    sudo openssl genrsa -out /etc/pki/CA/cakey.pem 2048
    sudo openssl req -new -x509 -days 3650 -key /etc/pki/CA/cakey.pem -out /etc/pki/CA/cacert.pem -subj "/C=US/ST=State/L=City/O=Organization/CN=Infron-CA"
    echo "CA certificate created at /etc/pki/CA/cacert.pem"
else
    echo "CA certificate already exists at /etc/pki/CA/cacert.pem"
fi

# Set proper permissions
sudo chmod 600 /etc/pki/CA/cakey.pem
sudo chmod 644 /etc/pki/CA/cacert.pem

# Create libvirt server certificates if they don't exist
if [ ! -f /etc/pki/libvirt/servercert.pem ]; then
    echo "Creating libvirt server certificates..."
    sudo openssl genrsa -out /etc/pki/libvirt/serverkey.pem 2048
    sudo openssl req -new -key /etc/pki/libvirt/serverkey.pem -out /etc/pki/libvirt/serverreq.pem -subj "/C=US/ST=State/L=City/O=Organization/CN=$(hostname)"
    sudo openssl x509 -req -days 3650 -in /etc/pki/libvirt/serverreq.pem -CA /etc/pki/CA/cacert.pem -CAkey /etc/pki/CA/cakey.pem -CAcreateserial -out /etc/pki/libvirt/servercert.pem
    echo "Libvirt server certificates created"
else
    echo "Libvirt server certificates already exist"
fi

# Set proper permissions for libvirt certificates
sudo chmod 600 /etc/pki/libvirt/serverkey.pem
sudo chmod 644 /etc/pki/libvirt/servercert.pem

# Create client certificates if they don't exist
if [ ! -f /etc/pki/libvirt/clientcert.pem ]; then
    echo "Creating libvirt client certificates..."
    sudo openssl genrsa -out /etc/pki/libvirt/clientkey.pem 2048
    sudo openssl req -new -key /etc/pki/libvirt/clientkey.pem -out /etc/pki/libvirt/clientreq.pem -subj "/C=US/ST=State/L=City/O=Organization/CN=client"
    sudo openssl x509 -req -days 3650 -in /etc/pki/libvirt/clientreq.pem -CA /etc/pki/CA/cacert.pem -CAkey /etc/pki/CA/cakey.pem -CAcreateserial -out /etc/pki/libvirt/clientcert.pem
    echo "Libvirt client certificates created"
else
    echo "Libvirt client certificates already exist"
fi

# Set proper permissions for client certificates
sudo chmod 600 /etc/pki/libvirt/clientkey.pem
sudo chmod 644 /etc/pki/libvirt/clientcert.pem

echo "Libvirt certificate setup completed!"
echo ""
echo "To use TLS connections with libvirt, you may need to:"
echo "1. Configure libvirtd to use TLS in /etc/libvirt/libvirtd.conf"
echo "2. Restart libvirtd service: sudo systemctl restart libvirtd"
echo "3. Update your libvirt connection URI to use TLS: qemu+tls://host/system"
