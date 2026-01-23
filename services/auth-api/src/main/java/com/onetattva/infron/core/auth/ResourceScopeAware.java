package com.onetattva.infron.core.auth;

import com.onetattva.infron.api.enums.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
