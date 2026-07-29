package com.yorel.muxon.auth;

import com.yorel.muxon.api.enums.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
