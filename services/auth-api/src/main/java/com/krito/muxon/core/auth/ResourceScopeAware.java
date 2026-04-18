package com.krito.muxon.core.auth;

import com.krito.muxon.api.enums.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
