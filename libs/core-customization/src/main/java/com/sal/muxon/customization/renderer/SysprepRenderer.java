package com.sal.muxon.customization.renderer;

import com.sal.muxon.customization.model.*;

import java.util.*;

/**
 * Renders a Windows {@code Autounattend.xml} for sysprep-based guest customization.
 *
 * <p>The file is placed on an ISO with volume label {@code UNATTEND}.
 * Windows Setup automatically picks it up from removable media at OOBE.
 *
 * <p><strong>Passes generated:</strong>
 * <ul>
 *   <li>{@code specialize} – ComputerName, TimeZone, Product Key, TCP/IP,
 *       domain/workgroup join, pre-scripts via {@code RunSynchronousCommand}</li>
 *   <li>{@code oobeSystem} – OOBE settings, AdminPassword, AutoLogon,
 *       FirstLogonCommands (post-scripts)</li>
 * </ul>
 */
public class SysprepRenderer implements CustomizationRenderer {

    private static final String VOLUME_LABEL = "UNATTEND";

    @Override
    public String volumeLabel() {
        return VOLUME_LABEL;
    }

    @Override
    public Map<String, String> render(String vmId, String vmName, VmCustomizationSpec spec) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("Autounattend.xml", renderAutounattend(vmName, spec));
        return files;
    }

    private String renderAutounattend(String vmName, VmCustomizationSpec spec) {
        String hostname = resolveHostname(vmName, spec);
        // Windows NetBIOS limit: 15 chars
        if (hostname.length() > 15) {
            hostname = hostname.substring(0, 15);
        }

        WindowsCustomizationSpec win = spec.getWindows();
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<unattend xmlns=\"urn:schemas-microsoft-com:unattend\">\n");

        // ── specialize pass ─────────────────────────────────────────────────
        xml.append("  <settings pass=\"specialize\">\n");

        // Shell-Setup: ComputerName, TimeZone, ProductKey
        xml.append("    <component name=\"Microsoft-Windows-Shell-Setup\"\n");
        xml.append("               processorArchitecture=\"amd64\"\n");
        xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
        xml.append("               language=\"neutral\"\n");
        xml.append("               versionScope=\"nonSxS\"\n");
        xml.append("               xmlns:wcm=\"http://schemas.microsoft.com/WMIConfig/2002/State\">\n");
        xml.append("      <ComputerName>").append(xmlEsc(hostname)).append("</ComputerName>\n");
        if (spec.getTimezone() != null && !spec.getTimezone().isBlank()) {
            xml.append("      <TimeZone>").append(xmlEsc(spec.getTimezone())).append("</TimeZone>\n");
        }
        if (win != null && win.getProductKey() != null && !win.getProductKey().isBlank()) {
            xml.append("      <ProductKey>").append(xmlEsc(win.getProductKey())).append("</ProductKey>\n");
        }
        xml.append("    </component>\n");

        // TCP/IP: static or DHCP per NIC
        if (spec.getNics() != null && !spec.getNics().isEmpty()) {
            xml.append("    <component name=\"Microsoft-Windows-TCPIP\"\n");
            xml.append("               processorArchitecture=\"amd64\"\n");
            xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
            xml.append("               language=\"neutral\"\n");
            xml.append("               versionScope=\"nonSxS\">\n");
            xml.append("      <Interfaces>\n");
            for (int i = 0; i < spec.getNics().size(); i++) {
                NicCustomizationSpec nic = spec.getNics().get(i);
                xml.append("        <Interface wcm:action=\"add\">\n");
                xml.append("          <Identifier>Local Area Connection " + (i + 1) + "</Identifier>\n");
                if (nic.isStatic()) {
                    xml.append("          <Ipv4Settings>\n");
                    xml.append("            <DhcpEnabled>false</DhcpEnabled>\n");
                    xml.append("          </Ipv4Settings>\n");
                    xml.append("          <UnicastIpAddresses>\n");
                    xml.append("            <IpAddress wcm:action=\"add\" wcm:keyValue=\"1\">")
                       .append(xmlEsc(nic.getIpAddress())).append("/").append(nic.getPrefix())
                       .append("</IpAddress>\n");
                    xml.append("          </UnicastIpAddresses>\n");
                    if (nic.getGateway() != null && !nic.getGateway().isBlank()) {
                        xml.append("          <Routes>\n");
                        xml.append("            <Route wcm:action=\"add\">\n");
                        xml.append("              <Identifier>0</Identifier>\n");
                        xml.append("              <Prefix>0.0.0.0/0</Prefix>\n");
                        xml.append("              <NextHopAddress>").append(xmlEsc(nic.getGateway())).append("</NextHopAddress>\n");
                        xml.append("            </Route>\n");
                        xml.append("          </Routes>\n");
                    }
                } else {
                    xml.append("          <Ipv4Settings>\n");
                    xml.append("            <DhcpEnabled>true</DhcpEnabled>\n");
                    xml.append("          </Ipv4Settings>\n");
                }
                xml.append("        </Interface>\n");
            }
            xml.append("      </Interfaces>\n");
            xml.append("    </component>\n");
        }

        // DNS client
        List<String> dns = spec.getDnsServers();
        if (dns != null && !dns.isEmpty()) {
            xml.append("    <component name=\"Microsoft-Windows-DNS-Client\"\n");
            xml.append("               processorArchitecture=\"amd64\"\n");
            xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
            xml.append("               language=\"neutral\"\n");
            xml.append("               versionScope=\"nonSxS\">\n");
            xml.append("      <Interfaces>\n");
            xml.append("        <Interface wcm:action=\"add\">\n");
            xml.append("          <Identifier>Local Area Connection 1</Identifier>\n");
            xml.append("          <DNSServerSearchOrder>\n");
            for (int i = 0; i < dns.size(); i++) {
                xml.append("            <IpAddress wcm:action=\"add\" wcm:keyValue=\"")
                   .append(i + 1).append("\">").append(xmlEsc(dns.get(i))).append("</IpAddress>\n");
            }
            xml.append("          </DNSServerSearchOrder>\n");
            xml.append("        </Interface>\n");
            xml.append("      </Interfaces>\n");
            xml.append("    </component>\n");
        }

        // Domain or workgroup join
        if (win != null && win.getJoinDomain() != null) {
            WindowsDomainJoin dj = win.getJoinDomain();
            xml.append("    <component name=\"Microsoft-Windows-UnattendedJoin\"\n");
            xml.append("               processorArchitecture=\"amd64\"\n");
            xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
            xml.append("               language=\"neutral\"\n");
            xml.append("               versionScope=\"nonSxS\">\n");
            xml.append("      <Identification>\n");
            xml.append("        <JoinDomain>").append(xmlEsc(dj.getDomain())).append("</JoinDomain>\n");
            if (dj.getUsername() != null) {
                xml.append("        <Credentials>\n");
                xml.append("          <Username>").append(xmlEsc(dj.getUsername())).append("</Username>\n");
                xml.append("          <Domain>").append(xmlEsc(dj.getDomain())).append("</Domain>\n");
                if (dj.getPassword() != null) {
                    xml.append("          <Password>").append(xmlEsc(dj.getPassword())).append("</Password>\n");
                }
                xml.append("        </Credentials>\n");
            }
            if (dj.getOu() != null && !dj.getOu().isBlank()) {
                xml.append("        <MachineObjectOU>").append(xmlEsc(dj.getOu())).append("</MachineObjectOU>\n");
            }
            xml.append("      </Identification>\n");
            xml.append("    </component>\n");
        } else {
            String wg = (win != null && win.getWorkgroup() != null) ? win.getWorkgroup() : "WORKGROUP";
            xml.append("    <component name=\"Microsoft-Windows-UnattendedJoin\"\n");
            xml.append("               processorArchitecture=\"amd64\"\n");
            xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
            xml.append("               language=\"neutral\"\n");
            xml.append("               versionScope=\"nonSxS\">\n");
            xml.append("      <Identification>\n");
            xml.append("        <JoinWorkgroup>").append(xmlEsc(wg)).append("</JoinWorkgroup>\n");
            xml.append("      </Identification>\n");
            xml.append("    </component>\n");
        }

        // Pre-scripts (RunSynchronousCommand in specialize)
        List<String> preScripts = new ArrayList<>();
        if (win != null && win.getResolvedPreScripts() != null) preScripts.addAll(win.getResolvedPreScripts());
        if (!preScripts.isEmpty()) {
            xml.append("    <component name=\"Microsoft-Windows-Deployment\"\n");
            xml.append("               processorArchitecture=\"amd64\"\n");
            xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
            xml.append("               language=\"neutral\"\n");
            xml.append("               versionScope=\"nonSxS\">\n");
            xml.append("      <RunSynchronous>\n");
            for (int i = 0; i < preScripts.size(); i++) {
                xml.append("        <RunSynchronousCommand wcm:action=\"add\">\n");
                xml.append("          <Order>").append(i + 1).append("</Order>\n");
                xml.append("          <Path>").append(xmlEsc(preScripts.get(i))).append("</Path>\n");
                xml.append("          <WillReboot>Never</WillReboot>\n");
                xml.append("        </RunSynchronousCommand>\n");
            }
            xml.append("      </RunSynchronous>\n");
            xml.append("    </component>\n");
        }

        xml.append("  </settings>\n");

        // ── oobeSystem pass ──────────────────────────────────────────────────
        xml.append("  <settings pass=\"oobeSystem\">\n");
        xml.append("    <component name=\"Microsoft-Windows-Shell-Setup\"\n");
        xml.append("               processorArchitecture=\"amd64\"\n");
        xml.append("               publicKeyToken=\"31bf3856ad364e35\"\n");
        xml.append("               language=\"neutral\"\n");
        xml.append("               versionScope=\"nonSxS\">\n");

        // OOBE skip settings
        xml.append("      <OOBE>\n");
        xml.append("        <HideEULAPage>true</HideEULAPage>\n");
        xml.append("        <HideLocalAccountScreen>true</HideLocalAccountScreen>\n");
        xml.append("        <HideOnlineAccountScreens>true</HideOnlineAccountScreens>\n");
        xml.append("        <HideWirelessSetupInOOBE>true</HideWirelessSetupInOOBE>\n");
        xml.append("        <ProtectYourPC>3</ProtectYourPC>\n");
        xml.append("        <SkipMachineOOBE>true</SkipMachineOOBE>\n");
        xml.append("        <SkipUserOOBE>true</SkipUserOOBE>\n");
        xml.append("      </OOBE>\n");

        // UserAccounts / AdminPassword
        if (win != null && win.getAdminPassword() != null && !win.getAdminPassword().isBlank()) {
            xml.append("      <UserAccounts>\n");
            xml.append("        <AdministratorPassword>\n");
            // Microsoft encodes as Base64(password + "AdministratorPassword")
            String encoded = encodeAdminPassword(win.getAdminPassword());
            xml.append("          <Value>").append(encoded).append("</Value>\n");
            xml.append("          <PlainText>false</PlainText>\n");
            xml.append("        </AdministratorPassword>\n");
            xml.append("      </UserAccounts>\n");
        }

        // AutoLogon
        if (win != null && win.getAutoLogonCount() > 0 && win.getAdminPassword() != null) {
            xml.append("      <AutoLogon>\n");
            xml.append("        <Password>\n");
            xml.append("          <Value>").append(xmlEsc(win.getAdminPassword())).append("</Value>\n");
            xml.append("          <PlainText>true</PlainText>\n");
            xml.append("        </Password>\n");
            xml.append("        <Username>Administrator</Username>\n");
            xml.append("        <Enabled>true</Enabled>\n");
            xml.append("        <LogonCount>").append(win.getAutoLogonCount()).append("</LogonCount>\n");
            xml.append("      </AutoLogon>\n");
        }

        // FirstLogonCommands (post-scripts + inline commands)
        List<String> postCmds = new ArrayList<>();
        if (win != null && win.getFirstLogonCommands() != null) postCmds.addAll(win.getFirstLogonCommands());
        if (win != null && win.getResolvedPostScripts() != null) postCmds.addAll(win.getResolvedPostScripts());
        if (!postCmds.isEmpty()) {
            xml.append("      <FirstLogonCommands>\n");
            for (int i = 0; i < postCmds.size(); i++) {
                xml.append("        <SynchronousCommand wcm:action=\"add\">\n");
                xml.append("          <Order>").append(i + 1).append("</Order>\n");
                xml.append("          <CommandLine>").append(xmlEsc(postCmds.get(i))).append("</CommandLine>\n");
                xml.append("          <RequiresUserInput>false</RequiresUserInput>\n");
                xml.append("        </SynchronousCommand>\n");
            }
            xml.append("      </FirstLogonCommands>\n");
        }

        // Locale / InputLocale
        if (win != null) {
            xml.append("      <UILanguage>").append(xmlEsc(win.getUiLanguage())).append("</UILanguage>\n");
            xml.append("      <SystemLocale>").append(xmlEsc(win.getSystemLocale())).append("</SystemLocale>\n");
            xml.append("      <UserLocale>").append(xmlEsc(win.getUserLocale())).append("</UserLocale>\n");
            xml.append("      <InputLocale>").append(xmlEsc(win.getInputLocale())).append("</InputLocale>\n");
        }

        xml.append("    </component>\n");
        xml.append("  </settings>\n");
        xml.append("</unattend>\n");

        return xml.toString();
    }

    private String resolveHostname(String vmName, VmCustomizationSpec spec) {
        if (spec.getHostname() != null && !spec.getHostname().isBlank()) {
            return spec.getHostname();
        }
        return vmName != null ? vmName : "MUXON-VM";
    }

    /**
     * Microsoft's AdminPassword encoding: Base64(password + "AdministratorPassword").
     * This is obfuscation, not encryption; the seed ISO is short-lived and stored at 0600.
     */
    private static String encodeAdminPassword(String password) {
        String toEncode = password + "AdministratorPassword";
        return Base64.getEncoder().encodeToString(toEncode.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));
    }

    private static String xmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
