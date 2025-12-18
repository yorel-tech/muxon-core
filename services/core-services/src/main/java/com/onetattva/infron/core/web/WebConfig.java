package com.onetattva.infron.core.web;


import com.onetattva.infron.core.auth.PermissionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantAccessInterceptor tenantAccessInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    @Autowired
    public WebConfig(TenantAccessInterceptor tenantAccessInterceptor, PermissionInterceptor permissionInterceptor) {
        this.tenantAccessInterceptor = tenantAccessInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // apply permission check to all API routes
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**");
        // apply to tenant routes
        registry.addInterceptor(tenantAccessInterceptor)
                .addPathPatterns("/api/tenant/**");
    }
}
