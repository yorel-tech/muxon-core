package com.scal.muxon.auth;

import com.scal.muxon.api.enums.RoleScopeType;

public interface ResourceScopeAware {
    RoleScopeType getScope();
    String getId(); // optional
}
