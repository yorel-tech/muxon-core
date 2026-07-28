package com.scal.muxon.customization.renderer;

import com.scal.muxon.customization.model.VmCustomizationSpec;

/**
 * Picks the correct {@link CustomizationRenderer} based on the VM's OS family.
 */
public final class CustomizationRendererFactory {

    private static final CloudInitRenderer CLOUD_INIT = new CloudInitRenderer();
    private static final SysprepRenderer SYSPREP = new SysprepRenderer();

    private CustomizationRendererFactory() {}

    public static CustomizationRenderer forSpec(VmCustomizationSpec spec) {
        if (spec.isWindows()) {
            return SYSPREP;
        }
        return CLOUD_INIT;
    }
}
