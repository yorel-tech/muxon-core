package com.onetattva.infron.core.orch.grpc;

import com.onetattva.infron.api.enums.EntityType;  // CONTENT_LIBRARY added in V39
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.core.orch.orch.JobService;
import com.onetattva.infron.grpc.workflow.v1.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC implementation of {@link ContentLibraryWorkflowServiceGrpc.ContentLibraryWorkflowServiceImplBase}.
 */
@GrpcService
public class ContentLibraryWorkflowGrpcService
        extends ContentLibraryWorkflowServiceGrpc.ContentLibraryWorkflowServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(ContentLibraryWorkflowGrpcService.class);

    private final JobService jobService;

    public ContentLibraryWorkflowGrpcService(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void replicateContentItem(ReplicateContentItemRequest request,
                                     StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sourceGrantId", request.getSourceGrantId());
            payload.put("targetGrantId", request.getTargetGrantId());
            return jobService.createJob(
                    JobType.CONTENT_LIBRARY_REPLICATE,
                    EntityType.CONTENT_LIBRARY,
                    UUID.fromString(request.getContentItemId()),
                    request.getCorrelationId(),
                    "CONTENT_ITEM_REPLICATE_COMMAND", payload);
        });
    }

    @Override
    public void importContentItem(ImportContentItemRequest request,
                                  StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("grantId", request.getGrantId());
            return jobService.createJob(
                    JobType.CONTENT_ITEM_FETCH,
                    EntityType.CONTENT_LIBRARY,
                    UUID.fromString(request.getContentItemId()),
                    request.getCorrelationId(),
                    "CONTENT_ITEM_IMPORT_COMMAND", payload);
        });
    }

    @Override
    public void deleteContentItem(DeleteContentItemRequest request,
                                  StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("grantId", request.getGrantId());
            return jobService.createJob(
                    JobType.CONTENT_LIBRARY_SYNC,
                    EntityType.CONTENT_LIBRARY,
                    UUID.fromString(request.getContentItemId()),
                    request.getCorrelationId(),
                    "CONTENT_ITEM_DELETE_COMMAND", payload);
        });
    }

    @FunctionalInterface
    private interface JobSupplier {
        JobResponse get() throws Exception;
    }

    private void handleRequest(StreamObserver<JobResponse> obs, JobSupplier supplier) {
        try {
            obs.onNext(supplier.get());
            obs.onCompleted();
        } catch (IllegalArgumentException e) {
            obs.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC error in ContentLibraryWorkflowGrpcService", e);
            obs.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
