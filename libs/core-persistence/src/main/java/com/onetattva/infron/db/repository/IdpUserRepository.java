package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.IdpUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdpUserRepository extends JpaRepository<IdpUserEntity, java.util.UUID> {

    /**
     * Find idp_user by identity provider and external id
     */
    @Query("SELECT u FROM IdpUserEntity u WHERE u.identityProviderId = :idpId AND u.externalId = :externalId")
    IdpUserEntity findByIdentityProviderIdAndExternalId(@Param("idpId") java.util.UUID idpId, @Param("externalId") String externalId);

    /**
     * Find idp_user by identity provider
     */
    @Query("SELECT u FROM IdpUserEntity u WHERE u.identityProviderId = :idpId")
    java.util.List<IdpUserEntity> findByIdentityProviderId(@Param("idpId") java.util.UUID idpId);
}
