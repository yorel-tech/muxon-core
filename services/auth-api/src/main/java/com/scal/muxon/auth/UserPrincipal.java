package com.scal.muxon.auth;

import java.util.List;

public record UserPrincipal(String id, String username, List<String> roles) {
}
