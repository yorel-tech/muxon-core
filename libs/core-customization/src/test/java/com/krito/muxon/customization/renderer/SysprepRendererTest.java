package com.krito.muxon.customization.renderer;

import com.krito.muxon.customization.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SysprepRendererTest {

    private final SysprepRenderer renderer = new SysprepRenderer();

    @Test
    void volumeLabelIsUnattend() {
        assertEquals("UNATTEND", renderer.volumeLabel());
    }

    @Test
    void renderProducesAutounattendXml() {
        VmCustomizationSpec spec = windowsSpec("WIN-TEST");
        Map<String, String> files = renderer.render("vm-uuid", "WIN-TEST", spec);

        assertTrue(files.containsKey("Autounattend.xml"), "must produce Autounattend.xml");
    }

    @Test
    void renderXml_hasXmlDeclarationAndRootElement() {
        String xml = renderXml(windowsSpec("WIN-TEST"), "WIN-TEST");
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>"),
                "must start with XML declaration");
        assertTrue(xml.contains("<unattend xmlns=\"urn:schemas-microsoft-com:unattend\">"),
                "must have <unattend> root");
        assertTrue(xml.endsWith("</unattend>\n"), "must close <unattend>");
    }

    @Test
    void renderXml_computerNameFromSpec() {
        VmCustomizationSpec spec = windowsSpec("MY-HOST");
        spec.setHostname("MY-HOST");
        String xml = renderXml(spec, "IGNORED-VM-NAME");

        assertTrue(xml.contains("<ComputerName>MY-HOST</ComputerName>"),
                "must use hostname from spec");
    }

    @Test
    void renderXml_hostnameFromVmNameWhenSpecHostnameBlank() {
        VmCustomizationSpec spec = windowsSpec(null);
        spec.setHostname(null);
        String xml = renderXml(spec, "VM-NAME");

        assertTrue(xml.contains("<ComputerName>VM-NAME</ComputerName>"),
                "must fall back to VM name");
    }

    @Test
    void renderXml_hostnameClampedTo15Chars() {
        VmCustomizationSpec spec = windowsSpec("VERY-LONG-HOSTNAME-EXCEEDS-15");
        spec.setHostname("VERY-LONG-HOSTNAME-EXCEEDS-15");
        String xml = renderXml(spec, "vm");

        assertTrue(xml.contains("<ComputerName>VERY-LONG-HOSTN</ComputerName>"),
                "hostname must be clamped to 15 characters");
    }

    @Test
    void renderXml_timezone() {
        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setTimezone("Pacific Standard Time");
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<TimeZone>Pacific Standard Time</TimeZone>"), "must include timezone");
    }

    @Test
    void renderXml_productKey() {
        WindowsCustomizationSpec win = new WindowsCustomizationSpec();
        win.setProductKey("XXXXX-XXXXX-XXXXX-XXXXX-XXXXX");

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(win);
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<ProductKey>XXXXX-XXXXX-XXXXX-XXXXX-XXXXX</ProductKey>"),
                "must include product key");
    }

    @Test
    void renderXml_adminPassword_encoded() {
        WindowsCustomizationSpec win = new WindowsCustomizationSpec();
        win.setAdminPassword("P@ssw0rd!");

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(win);
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<AdministratorPassword>"), "must include AdministratorPassword block");
        assertTrue(xml.contains("<PlainText>false</PlainText>"), "must mark password as encoded");
        // Password must NOT appear in plaintext
        assertFalse(xml.contains("P@ssw0rd!"), "plaintext password must not appear in XML");
    }

    @Test
    void renderXml_domainJoin() {
        WindowsDomainJoin dj = new WindowsDomainJoin();
        dj.setDomain("CORP");
        dj.setUsername("SVC_JOIN");
        dj.setPassword("secret123");

        WindowsCustomizationSpec win = new WindowsCustomizationSpec();
        win.setJoinDomain(dj);

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(win);
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<JoinDomain>CORP</JoinDomain>"), "must include JoinDomain");
        assertTrue(xml.contains("<Username>SVC_JOIN</Username>"), "must include join username");
    }

    @Test
    void renderXml_workgroupDefault_whenNoDomainJoin() {
        VmCustomizationSpec spec = windowsSpec("WIN");
        // No joinDomain set
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<JoinWorkgroup>WORKGROUP</JoinWorkgroup>"),
                "must join WORKGROUP when no domain specified");
    }

    @Test
    void renderXml_staticNic() {
        NicCustomizationSpec nic = new NicCustomizationSpec();
        nic.setIpAllocation("static");
        nic.setIpAddress("192.168.1.100");
        nic.setPrefix(24);
        nic.setGateway("192.168.1.1");

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setNics(List.of(nic));
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<DhcpEnabled>false</DhcpEnabled>"), "static NIC must disable DHCP");
        assertTrue(xml.contains("192.168.1.100/24"), "must include IP/prefix");
        assertTrue(xml.contains("<NextHopAddress>192.168.1.1</NextHopAddress>"), "must include gateway");
    }

    @Test
    void renderXml_dhcpNic() {
        NicCustomizationSpec nic = new NicCustomizationSpec();
        nic.setIpAllocation("dhcp");

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setNics(List.of(nic));
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<DhcpEnabled>true</DhcpEnabled>"), "DHCP NIC must enable DHCP");
    }

    @Test
    void renderXml_firstLogonCommands() {
        WindowsCustomizationSpec win = new WindowsCustomizationSpec();
        win.setFirstLogonCommands(List.of("powershell.exe -Command Set-ExecutionPolicy RemoteSigned"));
        win.setResolvedPostScripts(List.of("C:\\scripts\\post-deploy.ps1"));

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(win);
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<FirstLogonCommands>"), "must have FirstLogonCommands");
        assertTrue(xml.contains("Set-ExecutionPolicy"), "must include inline command");
        assertTrue(xml.contains("post-deploy.ps1"), "must include post script");
    }

    @Test
    void renderXml_preScripts_inSpecialize() {
        WindowsCustomizationSpec win = new WindowsCustomizationSpec();
        win.setResolvedPreScripts(List.of("cmd.exe /c echo pre-setup-done"));

        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(win);
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<RunSynchronous>"), "must include RunSynchronous for pre-scripts");
        assertTrue(xml.contains("pre-setup-done"), "must include pre-script command");
    }

    @Test
    void renderXml_xmlEscaping_specialChars() {
        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setTimezone("<Bad & Timezone>");
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("&lt;Bad &amp; Timezone&gt;"), "special chars must be XML-escaped");
    }

    @Test
    void renderXml_oobePass_alwaysPresent() {
        String xml = renderXml(windowsSpec("WIN"), "WIN");

        assertTrue(xml.contains("<settings pass=\"oobeSystem\">"), "must have oobeSystem pass");
        assertTrue(xml.contains("<HideEULAPage>true</HideEULAPage>"), "must skip EULA");
        assertTrue(xml.contains("<SkipMachineOOBE>true</SkipMachineOOBE>"), "must skip OOBE");
    }

    @Test
    void renderXml_localeDefaults() {
        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setWindows(new WindowsCustomizationSpec()); // triggers locale output
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("<UILanguage>en-US</UILanguage>"), "default UILanguage must be en-US");
        assertTrue(xml.contains("<SystemLocale>en-US</SystemLocale>"), "default SystemLocale must be en-US");
    }

    @Test
    void renderXml_dnsServers() {
        VmCustomizationSpec spec = windowsSpec("WIN");
        spec.setDnsServers(List.of("8.8.8.8", "8.8.4.4"));
        String xml = renderXml(spec, "WIN");

        assertTrue(xml.contains("Microsoft-Windows-DNS-Client"), "must include DNS-Client component");
        assertTrue(xml.contains("8.8.8.8"), "must include primary DNS");
        assertTrue(xml.contains("8.8.4.4"), "must include secondary DNS");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String renderXml(VmCustomizationSpec spec, String vmName) {
        return renderer.render("vm-uuid", vmName, spec).get("Autounattend.xml");
    }

    private static VmCustomizationSpec windowsSpec(String hostname) {
        VmCustomizationSpec spec = new VmCustomizationSpec();
        spec.setOsFamily("windows");
        spec.setHostname(hostname);
        return spec;
    }
}
