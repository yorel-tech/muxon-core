package com.yorel.muxon.services.content;

import com.yorel.muxon.api.model.VmTemplateDiskSpec;
import com.yorel.muxon.api.model.VmTemplateSpec;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class VmTemplateValidator {

    public void validate(VmTemplateSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("templateSpec is required for vm_template");
        }
        if (spec.getMetadata() == null || spec.getMetadata().getName() == null || spec.getMetadata().getName().isBlank()) {
            throw new IllegalArgumentException("templateSpec.metadata.name is required");
        }
        if (spec.getSpec() == null) {
            throw new IllegalArgumentException("templateSpec.spec is required");
        }
        if (spec.getSpec().getFirmware() == null) {
            throw new IllegalArgumentException("templateSpec.spec.firmware is required");
        }
        if (spec.getSpec().getCompute() == null) {
            throw new IllegalArgumentException("templateSpec.spec.compute is required");
        }
        if (spec.getSpec().getCompute().getCpuCores() == null || spec.getSpec().getCompute().getCpuCores() < 1) {
            throw new IllegalArgumentException("templateSpec.spec.compute.cpuCores must be >= 1");
        }
        if (spec.getSpec().getCompute().getMemoryMB() == null || spec.getSpec().getCompute().getMemoryMB() < 512) {
            throw new IllegalArgumentException("templateSpec.spec.compute.memoryMB must be >= 512");
        }
        if (spec.getSpec().getDisks() == null || spec.getSpec().getDisks().isEmpty()) {
            throw new IllegalArgumentException("templateSpec.spec.disks must have at least 1 disk");
        }
        if (spec.getSpec().getNetwork() == null || spec.getSpec().getNetwork().isEmpty()) {
            throw new IllegalArgumentException("templateSpec.spec.network must have at least 1 interface");
        }
        if (spec.getSpec().getCustomization() == null) {
            throw new IllegalArgumentException("templateSpec.spec.customization is required");
        }
        if (spec.getSpec().getCustomization().getType() == null) {
            throw new IllegalArgumentException("templateSpec.spec.customization.type is required");
        }
        if (spec.getSpec().getCustomization().getOsFamily() == null) {
            throw new IllegalArgumentException("templateSpec.spec.customization.osFamily is required");
        }

        validateDisks(spec.getSpec().getDisks());
    }

    private static void validateDisks(List<VmTemplateDiskSpec> disks) {
        Set<String> diskIds = new HashSet<>();
        Set<Integer> bootOrders = new HashSet<>();
        boolean hasBootOrder1 = false;

        for (VmTemplateDiskSpec d : disks) {
            if (d == null) {
                throw new IllegalArgumentException("templateSpec.spec.disks contains null entry");
            }
            if (d.getId() == null || d.getId().isBlank()) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].id is required");
            }
            if (!diskIds.add(d.getId())) {
                throw new IllegalArgumentException("Duplicate disk id: " + d.getId());
            }
            if (d.getFormat() == null || d.getFormat().isBlank()) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].format is required");
            }
            if (d.getBus() == null || d.getBus().isBlank()) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].bus is required");
            }
            if (d.getBootOrder() == null || d.getBootOrder() < 1) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].bootOrder must be >= 1");
            }
            if (!bootOrders.add(d.getBootOrder())) {
                throw new IllegalArgumentException("Duplicate bootOrder: " + d.getBootOrder());
            }
            if (Objects.equals(d.getBootOrder(), 1)) {
                hasBootOrder1 = true;
            }
            if (d.getSizeBytes() == null || d.getSizeBytes() < 1) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].sizeBytes must be positive");
            }
            if (d.getReadonly() == null) {
                throw new IllegalArgumentException("templateSpec.spec.disks[].readonly is required");
            }
        }

        if (!hasBootOrder1) {
            throw new IllegalArgumentException("At least one disk must have bootOrder=1");
        }

        // Require sequential bootOrder values 1..N (no gaps)
        int max = bootOrders.stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int i = 1; i <= max; i++) {
            if (!bootOrders.contains(i)) {
                throw new IllegalArgumentException("bootOrder values must be sequential starting at 1 (missing " + i + ")");
            }
        }
    }
}

