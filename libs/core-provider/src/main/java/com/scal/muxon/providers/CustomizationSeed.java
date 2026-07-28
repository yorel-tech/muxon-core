package com.scal.muxon.providers;

/**
 * Represents a seed ISO artifact to be attached to a VM as a CD-ROM for guest customization.
 *
 * @param isoPath           Absolute path to the seed ISO on the provider host filesystem.
 * @param volumeLabel       ISO volume label: {@code CIDATA} for Linux cloud-init,
 *                          {@code UNATTEND} for Windows sysprep.
 * @param osFamily          {@code linux} or {@code windows} — used by providers to pick the right bus/slot.
 */
public record CustomizationSeed(
        String isoPath,
        String volumeLabel,
        String osFamily
) {
    public boolean isLinux() {
        return "linux".equalsIgnoreCase(osFamily);
    }

    public boolean isWindows() {
        return "windows".equalsIgnoreCase(osFamily);
    }
}
