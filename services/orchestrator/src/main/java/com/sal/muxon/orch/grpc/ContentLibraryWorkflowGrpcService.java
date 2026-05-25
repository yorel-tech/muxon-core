package com.sal.muxon.orch.grpc;

import com.sal.muxon.api.enums.EntityType;  // CONTENT_LIBRARY added in V39
import com.sal.muxon.api.enums.JobType;
import com.sal.muxon.orch.orch.JobService;
import com.sal.muxon.grpc.workflow.v1.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC implementation of {@link ContentLibraryWorkflowServiceGrpc.ContentLibraryWorkflowServiceImplBase}.
 */
@Service
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
            log.debug(
                    "Content library workflow gRPC replicateContentItem: contentItemId={}, correlationId={}, "
                            + "sourceGrantIdSet={}, targetGrantIdSet={}",
                    request.getContentItemId(),
                    request.getCorrelationId(),
                    !request.getSourceGrantId().isBlank(),
                    !request.getTargetGrantId().isBlank());
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
            log.debug(
                    "Content library workflow gRPC importContentItem: contentItemId={}, correlationId={}, grantIdSet={}",
                    request.getContentItemId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank());
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
            log.debug(
                    "Content library workflow gRPC deleteContentItem: contentItemId={}, correlationId={}, grantIdSet={}",
                    request.getContentItemId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank());
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
