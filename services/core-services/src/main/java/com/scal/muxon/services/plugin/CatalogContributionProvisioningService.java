package com.scal.muxon.services.plugin;

import com.scal.muxon.api.enums.CatalogContributionStatus;
import com.scal.muxon.db.model.PluginCatalogContributionEntity;
import com.scal.muxon.db.repository.PluginCatalogContributionRepository;
import com.scal.muxon.db.repository.PluginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles provisioning flows for plugin-contributed catalog items.
 *
 * <p>Validation is delegated to the plugin via {@code CatalogContributionService.ValidateCatalogConfig}
 * before {@code ProvisionCatalogItem} is called. Status updates from the plugin
 * (PROVISIONING → ACTIVE | FAILED) are handled by {@code CatalogEntityEventConsumer}.
 */
@Service
public class CatalogContributionProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(CatalogContributionProvisioningService.class);

    @Autowired private PluginCatalogContributionRepository catalogContributionRepository;
    @Autowired private PluginRepository pluginRepository;
    @Autowired private PluginGrpcChannelFactory channelFactory;

    /**
     * Validates configuration and triggers provisioning of a plugin-contributed catalog item.
     *
     * @param catalogItemId  the ID of the plugin_catalog_contributions row
     * @param serviceInstanceId the platform-assigned service instance ID
     * @param configJson        tenant-supplied configuration JSON
     */
    @Transactional
    public void provision(UUID catalogItemId, UUID serviceInstanceId, String configJson) {
        PluginCatalogContributionEntity contribution = catalogContributionRepository.findById(catalogItemId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + catalogItemId));

        if (contribution.getStatus() != CatalogContributionStatus.ACTIVE) {
            throw new IllegalStateException("Catalog item " + contribution.getName() + " is not ACTIVE");
        }

        Map<String, Object> validationResult = channelFactory.validateCatalogConfig(
                contribution.getPlugin(), contribution.getName(), configJson);

        boolean valid = Boolean.TRUE.equals(validationResult.get("valid"));
        if (!valid) {
            throw new PluginValidationException(
                    "Plugin validation failed for catalog item " + contribution.getName()
                    + ": " + validationResult.get("errors"));
        }

        boolean accepted = channelFactory.provisionCatalogItem(
                contribution.getPlugin(), serviceInstanceId, contribution.getName(), configJson);

        if (!accepted) {
            throw new PluginRegistrationException("Plugin rejected provisioning of catalog item: " + contribution.getName());
        }

        log.info("Catalog item provisioning accepted: {} serviceInstanceId={}", contribution.getName(), serviceInstanceId);
    }

    /**
     * Triggers deprovisioning of a plugin-contributed service instance.
     */
    @Transactional
    public void deprovision(UUID catalogItemId, UUID serviceInstanceId, String externalId) {
        PluginCatalogContributionEntity contribution = catalogContributionRepository.findById(catalogItemId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + catalogItemId));

        boolean accepted = channelFactory.deprovisionCatalogItem(
                contribution.getPlugin(), serviceInstanceId, externalId);

        if (!accepted) {
            log.warn("Plugin rejected deprovision for serviceInstanceId={} — proceeding anyway", serviceInstanceId);
        }

        log.info("Catalog item deprovision triggered: {} serviceInstanceId={}", contribution.getName(), serviceInstanceId);
    }

    /**
     * Returns all ACTIVE catalog contributions visible in the platform catalog.
     */
    public List<PluginCatalogContributionEntity> listActiveCatalogContributions() {
        return catalogContributionRepository.findByStatus(CatalogContributionStatus.ACTIVE);
    }
}
