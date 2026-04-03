package com.onetattva.infron.core.config;

import com.onetattva.infron.grpc.vmconsole.v1.VmConsoleResolutionServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class OrchestratorGrpcClientConfig {

    @Bean
    VmConsoleResolutionServiceGrpc.VmConsoleResolutionServiceBlockingStub vmConsoleResolutionStub(
            GrpcChannelFactory channels) {
        return VmConsoleResolutionServiceGrpc.newBlockingStub(channels.createChannel("orchestrator"));
    }
}
