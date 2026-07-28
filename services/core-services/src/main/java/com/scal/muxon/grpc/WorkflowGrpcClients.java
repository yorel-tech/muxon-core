package com.scal.muxon.grpc;

import com.scal.muxon.grpc.workflow.v1.ContentLibraryWorkflowServiceGrpc;
import com.scal.muxon.grpc.workflow.v1.JobQueryServiceGrpc;
import com.scal.muxon.grpc.workflow.v1.VMWorkflowServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC client stubs for the three workflow services hosted by services/orchestrator.
 * All three stubs share a single managed channel (named {@code orchestrator}; configure via
 * {@code spring.grpc.client.channels.orchestrator.*}).
 *
 * <p>Example {@code application.yaml}:
 * <pre>
 * spring:
 *   grpc:
 *     client:
 *       channels:
 *         orchestrator:
 *           address: static://localhost:9090
 *           negotiation-type: PLAINTEXT
 * </pre>
 */
@Configuration
public class WorkflowGrpcClients {

    @Bean(destroyMethod = "shutdown")
    public ManagedChannel orchestratorChannel(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("orchestrator");
    }

    @Bean
    public VMWorkflowServiceGrpc.VMWorkflowServiceBlockingStub vmWorkflowStub(
            ManagedChannel orchestratorChannel) {
        return VMWorkflowServiceGrpc.newBlockingStub(orchestratorChannel);
    }

    @Bean
    public ContentLibraryWorkflowServiceGrpc.ContentLibraryWorkflowServiceBlockingStub
            contentLibraryWorkflowStub(ManagedChannel orchestratorChannel) {
        return ContentLibraryWorkflowServiceGrpc.newBlockingStub(orchestratorChannel);
    }

    @Bean
    public JobQueryServiceGrpc.JobQueryServiceBlockingStub jobQueryStub(
            ManagedChannel orchestratorChannel) {
        return JobQueryServiceGrpc.newBlockingStub(orchestratorChannel);
    }
}
