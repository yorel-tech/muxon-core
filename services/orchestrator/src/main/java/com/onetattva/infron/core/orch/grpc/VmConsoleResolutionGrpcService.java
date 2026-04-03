package com.onetattva.infron.core.orch.grpc;

import com.onetattva.infron.core.orch.wiring.TenantAwareVmProviderRegistry;
import com.onetattva.infron.core.providers.VmConsoleConnectionInfo;
import com.onetattva.infron.core.providers.VmConsoleRequest;
import com.onetattva.infron.core.providers.VmProvider;
import com.onetattva.infron.grpc.vmconsole.v1.ResolveVmConsoleRequest;
import com.onetattva.infron.grpc.vmconsole.v1.ResolveVmConsoleResponse;
import com.onetattva.infron.grpc.vmconsole.v1.VmConsoleResolutionServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.util.Optional;
import java.util.UUID;

/**
 * gRPC API used by core-services to resolve VM console endpoints on the provider (orchestrator-only).
 */
@GrpcService
public class VmConsoleResolutionGrpcService extends VmConsoleResolutionServiceGrpc.VmConsoleResolutionServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(VmConsoleResolutionGrpcService.class);

    private final TenantAwareVmProviderRegistry providerRegistry;

    public VmConsoleResolutionGrpcService(TenantAwareVmProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public void resolveVmConsole(ResolveVmConsoleRequest request, StreamObserver<ResolveVmConsoleResponse> responseObserver) {
        try {
            UUID grantId = UUID.fromString(request.getTenantDatacenterGrantId());
            UUID vmId = UUID.fromString(request.getVmId());
            String externalId = request.getExternalId();
            String nodeIdStr = request.getNodeId();
            UUID nodeId = nodeIdStr == null || nodeIdStr.isBlank() ? null : UUID.fromString(nodeIdStr);

            Optional<VmProvider> providerOpt = providerRegistry.resolveProviderForTenantDatacenter(grantId);
            if (providerOpt.isEmpty()) {
                respond(responseObserver, fail("No infrastructure provider is configured for this VM"));
                return;
            }

            VmConsoleRequest consoleRequest = new VmConsoleRequest(vmId, grantId, externalId, nodeId);
            VmConsoleConnectionInfo info;
            try {
                info = providerOpt.get().getConsoleConnection(consoleRequest).join();
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                log.warn("Console resolution failed for vm {}: {}", vmId, c.getMessage());
                respond(responseObserver, fail(c.getMessage() != null ? c.getMessage() : "Provider error"));
                return;
            }

            ResolveVmConsoleResponse.Builder b = ResolveVmConsoleResponse.newBuilder()
                    .setOk(true)
                    .setConsoleType(info.consoleType().name())
                    .setHost(info.host())
                    .setPort(info.port())
                    .setTls(info.tls());
            if (info.password() != null) {
                b.setPassword(info.password());
            }
            respond(responseObserver, b.build());
        } catch (IllegalArgumentException e) {
            respond(responseObserver, fail("Invalid request: " + e.getMessage()));
        } catch (Exception e) {
            log.error("resolveVmConsole error", e);
            respond(responseObserver, fail(e.getMessage() != null ? e.getMessage() : "Internal error"));
        }
    }

    private static ResolveVmConsoleResponse fail(String msg) {
        return ResolveVmConsoleResponse.newBuilder()
                .setOk(false)
                .setErrorMessage(msg != null ? msg : "Unknown error")
                .build();
    }

    private static void respond(StreamObserver<ResolveVmConsoleResponse> responseObserver, ResolveVmConsoleResponse response) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
