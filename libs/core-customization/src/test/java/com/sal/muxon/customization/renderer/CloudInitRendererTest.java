package com.sal.muxon.customization.renderer;

import com.sal.muxon.customization.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudInitRendererTest {

    private final CloudInitRenderer renderer = new CloudInitRenderer();

    @Test
    void volumeLabelIsCidata() {
        assertEquals("CIDATA", renderer.volumeLabel());
    }

    @Test
    void renderMetaData_containsInstanceIdAndHostname() {
        VmCustomizationSpec spec = linuxSpec("my-vm", null);
        Map<String, String> files = renderer.render("vm-uuid-123", "my-vm", spec);

        String metaData = files.get("meta-data");
        assertNotNull(metaData);
        assertTrue(metaData.contains("instance-id: vm-uuid-123"), "meta-data must contain instance-id");
        assertTrue(metaData.contains("local-hostname: my-vm"), "meta-data must contain local-hostname");
    }

    @Test
    void renderMetaData_hostnameFromSpecOverridesVmName() {
        VmCustomizationSpec spec = linuxSpec("override-host", null);
        spec.setHostname("override-host");
        Map<String, String> files = renderer.render("vm-1", "vm-name", spec);

        assertTrue(files.get("meta-data").contains("local-hostname: override-host"));
    }

    @Test
    void renderUserData_header() {
        VmCustomizationSpec spec = linuxSpec("vm", null);
        String userData = renderer.render("id", "vm", spec).get("user-data");
        assertTrue(userData.startsWith("#cloud-config\n"), "user-data must start with #cloud-config header");
    }

    @Test
    void renderUserData_hostnameAndTimezone() {
        VmCustomizationSpec spec = linuxSpec("web-01", null);
        spec.setHostname("web-01");
        spec.setTimezone("America/New_York");

        String userData = renderer.render("id", "vm", spec).get("user-data");
        assertTrue(userData.contains("hostname: web-01"), "must set hostname");
        assertTrue(userData.contains("timezone: America/New_York"), "must set timezone");
    }

    @Test
    void renderUserData_fqdn_whenDomainSet() {
        VmCustomizationSpec spec = linuxSpec("app-01", null);
        spec.setHostname("app-01");
        spec.setDomain("corp.example.com");

        String userData = renderer.render("id", "vm", spec).get("user-data");
        assertTrue(userData.contains("fqdn: app-01.corp.example.com"), "must set fqdn");
        assertTrue(userData.contains("manage_etc_hosts: true"));
    }

    @Test
    void renderUserData_users_sshKeys_sudo() {
        GuestUser user = new GuestUser();
        user.setName("adminuser");
        user.setShell("/bin/bash");
        user.setSudo("ALL=(ALL) NOPASSWD:ALL");
        user.setSshAuthorizedKeys(List.of("ssh-ed25519 AAAA... user@host"));

        LinuxCustomizationSpec linux = new LinuxCustomizationSpec();
        linux.setUsers(List.of(user));

        VmCustomizationSpec spec = linuxSpec("vm", linux);

        String userData = renderer.render("id", "vm", spec).get("user-data");
        assertTrue(userData.contains("- name: adminuser"), "must list user");
        assertTrue(userData.contains("shell: /bin/bash"), "must set shell");
        assertTrue(userData.contains("sudo: 'ALL=(ALL) NOPASSWD:ALL'"), "must set sudo (quoted because of colons)");
        assertTrue(userData.contains("ssh_authorized_keys:"), "must have ssh_authorized_keys section");
        assertTrue(userData.contains("ssh-ed25519"), "must include the SSH public key");
    }

    @Test
    void renderUserData_packages() {
        LinuxCustomizationSpec linux = new LinuxCustomizationSpec();
        linux.setPackages(List.of("nginx", "git", "curl"));

        VmCustomizationSpec spec = linuxSpec("vm", linux);
        String userData = renderer.render("id", "vm", spec).get("user-data");

        assertTrue(userData.contains("packages:"), "must have packages section");
        assertTrue(userData.contains("- nginx"));
        assertTrue(userData.contains("- git"));
        assertTrue(userData.contains("- curl"));
        assertTrue(userData.contains("package_update: true"));
    }

    @Test
    void renderUserData_runcmd_includesInlineAndResolvedScripts() {
        LinuxCustomizationSpec linux = new LinuxCustomizationSpec();
        linux.setRuncmd(List.of("systemctl enable nginx"));
        linux.setResolvedPostScripts(List.of("echo post-script-done"));

        VmCustomizationSpec spec = linuxSpec("vm", linux);
        String userData = renderer.render("id", "vm", spec).get("user-data");

        assertTrue(userData.contains("runcmd:"), "must have runcmd section");
        assertTrue(userData.contains("systemctl enable nginx"));
        assertTrue(userData.contains("echo post-script-done"));
    }

    @Test
    void renderUserData_bootcmd_includesPreScripts() {
        LinuxCustomizationSpec linux = new LinuxCustomizationSpec();
        linux.setResolvedPreScripts(List.of("echo pre-boot"));
        linux.setBootcmd(List.of("echo explicit-bootcmd"));

        VmCustomizationSpec spec = linuxSpec("vm", linux);
        String userData = renderer.render("id", "vm", spec).get("user-data");

        assertTrue(userData.contains("bootcmd:"), "must have bootcmd section");
        assertTrue(userData.contains("echo pre-boot"));
        assertTrue(userData.contains("echo explicit-bootcmd"));
    }

    @Test
    void renderNetworkConfig_dhcp_default() {
        VmCustomizationSpec spec = linuxSpec("vm", null);
        String netCfg = renderer.render("id", "vm", spec).get("network-config");

        assertTrue(netCfg.startsWith("version: 2\n"), "must start with version: 2");
        assertTrue(netCfg.contains("dhcp4: true"), "default NIC must be DHCP");
    }

    @Test
    void renderNetworkConfig_staticNic() {
        NicCustomizationSpec nic = new NicCustomizationSpec();
        nic.setIpAllocation("static");
        nic.setIpAddress("10.1.2.50");
        nic.setPrefix(24);
        nic.setGateway("10.1.2.1");
        nic.setDnsServers(List.of("10.1.0.1", "10.1.0.2"));

        VmCustomizationSpec spec = linuxSpec("vm", null);
        spec.setNics(List.of(nic));

        String netCfg = renderer.render("id", "vm", spec).get("network-config");

        assertTrue(netCfg.contains("dhcp4: false"), "static NIC must disable DHCP");
        assertTrue(netCfg.contains("10.1.2.50/24"), "must include IP/prefix");
        assertTrue(netCfg.contains("gateway4: 10.1.2.1"), "must include gateway");
        assertTrue(netCfg.contains("10.1.0.1"), "must include DNS servers");
    }

    @Test
    void renderNetworkConfig_multipleNics_mixedDhcpAndStatic() {
        NicCustomizationSpec nic0 = new NicCustomizationSpec();
        nic0.setIpAllocation("static");
        nic0.setIpAddress("10.0.0.10");
        nic0.setPrefix(24);
        nic0.setGateway("10.0.0.1");

        NicCustomizationSpec nic1 = new NicCustomizationSpec();
        nic1.setIpAllocation("dhcp");

        VmCustomizationSpec spec = linuxSpec("vm", null);
        spec.setNics(List.of(nic0, nic1));

        String netCfg = renderer.render("id", "vm", spec).get("network-config");

        assertTrue(netCfg.contains("en0"), "must define eth0/en0");
        assertTrue(netCfg.contains("en1"), "must define eth1/en1");
        // nic0 should be static, nic1 should be dhcp
        assertTrue(netCfg.contains("dhcp4: false"), "first nic must be static");
        assertTrue(netCfg.contains("dhcp4: true"), "second nic must be dhcp");
    }

    @Test
    void allThreeFilesPresent() {
        VmCustomizationSpec spec = linuxSpec("vm", null);
        Map<String, String> files = renderer.render("id", "vm", spec);

        assertTrue(files.containsKey("meta-data"), "seed must include meta-data");
        assertTrue(files.containsKey("user-data"), "seed must include user-data");
        assertTrue(files.containsKey("network-config"), "seed must include network-config");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static VmCustomizationSpec linuxSpec(String hostname, LinuxCustomizationSpec linux) {
        VmCustomizationSpec spec = new VmCustomizationSpec();
        spec.setOsFamily("linux");
        spec.setHostname(hostname);
        spec.setLinux(linux);
        return spec;
    }
}
