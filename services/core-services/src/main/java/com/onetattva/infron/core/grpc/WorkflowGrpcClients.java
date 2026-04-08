package com.onetattva.infron.core.grpc;

import com.onetattva.infron.grpc.workflow.v1.ContentLibraryWorkflowServiceGrpc;
import com.onetattva.infron.grpc.workflow.v1.JobQueryServiceGrpc;
import com.onetattva.infron.grpc.workflow.v1.VMWorkflowServiceGrpc;
import io.grpc.ManagedChannel;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC client stubs for the three workflow services hosted by services/orchestrator.
 * All three stubs share a single managed channel (configured via
 * {@code grpc.client.orchestrator.*} properties).
 *
 * <p>Example application.yml:
 * <pre>
 * grpc:
 *   client:
 *     orchestrator:
 *       address: static://localhost:9090
 *       negotiation-type: plaintext
 * </pre>
 */
@Configuration
public class WorkflowGrpcClients {

    @GrpcClient("orchestrator")
    private ManagedChannel orchestratorChannel;

    @Bean
    public VMWorkflowServiceGrpc.VMWorkflowServiceBlockingStub vmWorkflowStub() {
        return VMWorkflowServiceGrpc.newBlockingStub(orchestratorChannel);
    }

    @Bean
    public ContentLibraryWorkflowServiceGrpc.ContentLibraryWorkflowServiceBlockingStub
    contentLibraryWorkflowStub() {
        return ContentLibraryWorkflowServiceGrpc.newBlockingStub(orchestratorChannel);
    }

    @Bean
    public JobQueryServiceGrpc.JobQueryServiceBlockingStub jobQueryStub() {
        return JobQueryServiceGrpc.newBlockingStub(orchestratorChannel);
    }
}
