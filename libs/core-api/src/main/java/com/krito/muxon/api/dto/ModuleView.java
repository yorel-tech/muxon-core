package com.krito.muxon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * View model for exposing backend modules through the /api/v1/info endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleView {

    private String id;

    private String displayName;

    private String category;

    private Map<String, String> metadata;
}

