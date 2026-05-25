package com.sal.muxon.auth;

import com.sal.muxon.api.enums.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
