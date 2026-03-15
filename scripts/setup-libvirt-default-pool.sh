#!/bin/bash

# 1. Create the directory
sudo mkdir -p /var/lib/libvirt/vms

# 2. Set the ownership
# root owns the folder, but the libvirt-qemu group can access it
sudo chown root:libvirt-qemu /var/lib/libvirt/vms

# 3. Set permissions (775: owner/group can write, others can read)
sudo chmod 775 /var/lib/libvirt/vms

virsh pool-define-as --name default --type dir --target /var/lib/libvirt/vms
virsh pool-build default
virsh pool-start default
virsh pool-autostart default