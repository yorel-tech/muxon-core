package com.onetattva.infron.core.auth;

import com.onetattva.infron.db.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
