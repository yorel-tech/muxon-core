package com.scal.muxon.services.plugin;

public class PluginNotFoundException extends RuntimeException {
    public PluginNotFoundException(String message) {
        super(message);
    }
}
