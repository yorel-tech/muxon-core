package com.krito.muxon.customization.renderer;

import com.krito.muxon.customization.model.VmCustomizationSpec;

import java.util.Map;

/**
 * Renders a {@link VmCustomizationSpec} into a set of named files to be placed on the seed ISO.
 */
public interface CustomizationRenderer {

    /**
     * Render all files that should be placed on the seed ISO.
     *
     * @param vmId   VM UUID string (used as the cloud-init instance-id).
     * @param vmName VM name (used as the fallback hostname when spec.hostname is blank).
     * @param spec   Fully merged and secret-decrypted customization spec.
     * @return Map of filename → file content (UTF-8 string).
     */
    Map<String, String> render(String vmId, String vmName, VmCustomizationSpec spec);

    /** Volume label to apply to the seed ISO. */
    String volumeLabel();
}
