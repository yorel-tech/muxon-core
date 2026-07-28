package com.scal.muxon.customization.renderer;

import com.scal.muxon.customization.model.*;

import java.util.*;

/**
 * Renders cloud-init NoCloud datasource files for Linux VMs:
 * <ul>
 *   <li>{@code meta-data} – instance-id and local-hostname</li>
 *   <li>{@code user-data} – users, SSH keys, packages, runcmd, write_files, timezone</li>
 *   <li>{@code network-config} – v2 format with per-NIC static/DHCP</li>
 * </ul>
 *
 * <p>The ISO volume label must be {@code CIDATA} (all caps) for cloud-init to recognise it.
 */
public class CloudInitRenderer implements CustomizationRenderer {

    private static final String VOLUME_LABEL = "CIDATA";

    @Override
    public String volumeLabel() {
        return VOLUME_LABEL;
    }

    @Override
    public Map<String, String> render(String vmId, String vmName, VmCustomizationSpec spec) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("meta-data", renderMetaData(vmId, resolveHostname(vmName, spec)));
        files.put("user-data", renderUserData(vmName, spec));
        files.put("network-config", renderNetworkConfig(spec));
        return files;
    }

    // ── meta-data ─────────────────────────────────────────────────────────────

    private String renderMetaData(String vmId, String hostname) {
        return "instance-id: " + vmId + "\n" +
               "local-hostname: " + hostname + "\n";
    }

    // ── user-data ─────────────────────────────────────────────────────────────

    private String renderUserData(String vmName, VmCustomizationSpec spec) {
        StringBuilder sb = new StringBuilder("#cloud-config\n");

        // Hostname
        String hostname = resolveHostname(vmName, spec);
        sb.append("hostname: ").append(yamlScalar(hostname)).append("\n");
        if (spec.getDomain() != null && !spec.getDomain().isBlank()) {
            sb.append("fqdn: ").append(yamlScalar(hostname + "." + spec.getDomain())).append("\n");
            sb.append("manage_etc_hosts: true\n");
        }

        // Timezone
        if (spec.getTimezone() != null && !spec.getTimezone().isBlank()) {
            sb.append("timezone: ").append(yamlScalar(spec.getTimezone())).append("\n");
        }

        // DNS search domains (global)
        if (spec.getDnsSearch() != null && !spec.getDnsSearch().isEmpty()) {
            sb.append("resolv_conf:\n");
            sb.append("  searchdomains:\n");
            for (String s : spec.getDnsSearch()) {
                sb.append("    - ").append(yamlScalar(s)).append("\n");
            }
        }

        LinuxCustomizationSpec linux = spec.getLinux();

        // Users
        if (linux != null && linux.getUsers() != null && !linux.getUsers().isEmpty()) {
            sb.append("users:\n");
            sb.append("  - name: root\n");
            for (GuestUser u : linux.getUsers()) {
                sb.append("  - name: ").append(yamlScalar(u.getName())).append("\n");
                if (u.getGecos() != null) {
                    sb.append("    gecos: ").append(yamlScalar(u.getGecos())).append("\n");
                }
                if (u.getShell() != null) {
                    sb.append("    shell: ").append(u.getShell()).append("\n");
                }
                if (u.getGroups() != null && !u.getGroups().isEmpty()) {
                    sb.append("    groups: ").append(String.join(", ", u.getGroups())).append("\n");
                }
                if (u.getSudo() != null) {
                    sb.append("    sudo: ").append(yamlScalar(u.getSudo())).append("\n");
                }
                if (u.getSshAuthorizedKeys() != null && !u.getSshAuthorizedKeys().isEmpty()) {
                    sb.append("    ssh_authorized_keys:\n");
                    for (String key : u.getSshAuthorizedKeys()) {
                        sb.append("      - ").append(yamlScalar(key)).append("\n");
                    }
                }
                if (u.getPassword() != null && !u.getPassword().isBlank()) {
                    // Pass the raw password; cloud-init will hash it with SHA-512 via chpasswd.
                    sb.append("    lock_passwd: false\n");
                    sb.append("    passwd: ").append(yamlScalar(u.getPassword())).append("\n");
                }
            }
        }

        // Packages
        if (linux != null && linux.getPackages() != null && !linux.getPackages().isEmpty()) {
            sb.append("packages:\n");
            for (String pkg : linux.getPackages()) {
                sb.append("  - ").append(yamlScalar(pkg)).append("\n");
            }
            sb.append("package_update: true\n");
        }

        // write_files
        if (linux != null && linux.getWriteFiles() != null && !linux.getWriteFiles().isEmpty()) {
            sb.append("write_files:\n");
            for (var wf : linux.getWriteFiles()) {
                sb.append("  - path: ").append(yamlScalar(str(wf.get("path")))).append("\n");
                String enc = str(wf.getOrDefault("encoding", "plain"));
                sb.append("    encoding: ").append(enc).append("\n");
                sb.append("    content: |\n");
                for (String line : str(wf.get("content")).split("\n", -1)) {
                    sb.append("      ").append(line).append("\n");
                }
                sb.append("    permissions: ").append(yamlScalar(str(wf.getOrDefault("permissions", "0644")))).append("\n");
                sb.append("    owner: ").append(str(wf.getOrDefault("owner", "root:root"))).append("\n");
            }
        }

        // bootcmd (pre-network, early boot)
        List<String> bootcmds = new ArrayList<>();
        if (linux != null && linux.getResolvedPreScripts() != null) {
            bootcmds.addAll(linux.getResolvedPreScripts());
        }
        if (linux != null && linux.getBootcmd() != null) {
            bootcmds.addAll(linux.getBootcmd());
        }
        if (!bootcmds.isEmpty()) {
            sb.append("bootcmd:\n");
            for (String cmd : bootcmds) {
                sb.append("  - ").append(yamlScalar(cmd)).append("\n");
            }
        }

        // runcmd (final stage, after all modules)
        List<String> runcmds = new ArrayList<>();
        if (linux != null && linux.getRuncmd() != null) {
            runcmds.addAll(linux.getRuncmd());
        }
        if (linux != null && linux.getResolvedPostScripts() != null) {
            runcmds.addAll(linux.getResolvedPostScripts());
        }
        if (!runcmds.isEmpty()) {
            sb.append("runcmd:\n");
            for (String cmd : runcmds) {
                sb.append("  - ").append(yamlScalar(cmd)).append("\n");
            }
        }

        return sb.toString();
    }

    // ── network-config v2 ────────────────────────────────────────────────────

    private String renderNetworkConfig(VmCustomizationSpec spec) {
        StringBuilder sb = new StringBuilder("version: 2\n");
        sb.append("ethernets:\n");

        List<NicCustomizationSpec> nics = spec.getNics();
        if (nics == null || nics.isEmpty()) {
            // Default: first NIC on DHCP with match-by-name (will match eth0/ens3/etc. via name pattern)
            sb.append("  eth0:\n");
            sb.append("    match:\n");
            sb.append("      name: \"en*\"\n");
            sb.append("    dhcp4: true\n");
            appendGlobalDns(sb, "    ", spec);
            return sb.toString();
        }

        for (int i = 0; i < nics.size(); i++) {
            NicCustomizationSpec nic = nics.get(i);
            String iface = "eth" + i;
            sb.append("  ").append(iface).append(":\n");
            sb.append("    match:\n");
            sb.append("      name: \"").append(iface.replace("eth", "en")).append("*\"\n");

            if (nic.isStatic()) {
                sb.append("    dhcp4: false\n");
                sb.append("    addresses:\n");
                sb.append("      - ").append(nic.getIpAddress()).append("/").append(nic.getPrefix()).append("\n");
                if (nic.getGateway() != null && !nic.getGateway().isBlank()) {
                    sb.append("    gateway4: ").append(nic.getGateway()).append("\n");
                }
                List<String> dns = nic.getDnsServers() != null ? nic.getDnsServers() : spec.getDnsServers();
                if (dns != null && !dns.isEmpty()) {
                    sb.append("    nameservers:\n");
                    sb.append("      addresses: [").append(String.join(", ", dns)).append("]\n");
                    if (spec.getDnsSearch() != null && !spec.getDnsSearch().isEmpty()) {
                        sb.append("      search: [").append(String.join(", ", spec.getDnsSearch())).append("]\n");
                    }
                }
            } else {
                sb.append("    dhcp4: true\n");
                appendGlobalDns(sb, "    ", spec);
            }
        }
        return sb.toString();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void appendGlobalDns(StringBuilder sb, String indent, VmCustomizationSpec spec) {
        if (spec.getDnsServers() != null && !spec.getDnsServers().isEmpty()) {
            sb.append(indent).append("nameservers:\n");
            sb.append(indent).append("  addresses: [")
              .append(String.join(", ", spec.getDnsServers())).append("]\n");
        }
    }

    private String resolveHostname(String vmName, VmCustomizationSpec spec) {
        if (spec.getHostname() != null && !spec.getHostname().isBlank()) {
            return spec.getHostname();
        }
        return vmName != null ? vmName : "vm";
    }

    /** Wrap a string in single quotes if it contains special YAML characters. */
    private static String yamlScalar(String s) {
        if (s == null) return "null";
        if (s.contains(":") || s.contains("#") || s.contains("'") || s.contains("\"")
                || s.startsWith(" ") || s.endsWith(" ")) {
            return "'" + s.replace("'", "''") + "'";
        }
        return s;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
