-- Drop KUBERNETES from provider_type enum and clean related JSON data.
-- Existing Kubernetes providers are migrated to LIBVIRT so rows remain valid.

UPDATE provider SET type = 'LIBVIRT'::provider_type WHERE type::text = 'KUBERNETES';

UPDATE datacenter
SET settings = jsonb_set(settings::jsonb, '{providerType}', '"LIBVIRT"'::jsonb)
WHERE settings IS NOT NULL
  AND settings::jsonb->>'providerType' = 'KUBERNETES';

UPDATE storage_classes
SET allowed_providers = (
    SELECT COALESCE(jsonb_agg(to_jsonb(elem)), '[]'::jsonb)
    FROM jsonb_array_elements_text(allowed_providers) AS elem
    WHERE elem <> 'kubernetes'
)
WHERE allowed_providers IS NOT NULL;

ALTER TYPE provider_type RENAME TO provider_type_old;

CREATE TYPE provider_type AS ENUM ('PROXMOX', 'LIBVIRT');

ALTER TABLE provider
    ALTER COLUMN type TYPE provider_type USING (
        CASE type::text
            WHEN 'PROXMOX' THEN 'PROXMOX'::provider_type
            WHEN 'LIBVIRT' THEN 'LIBVIRT'::provider_type
            WHEN 'KUBERNETES' THEN 'LIBVIRT'::provider_type
            ELSE NULL
        END
    );

DROP TYPE provider_type_old;
