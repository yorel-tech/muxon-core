/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.grpc;

import com.yorel.muxon.grpc.workflow.v1.ContentLibraryWorkflowServiceGrpc;
import com.yorel.muxon.grpc.workflow.v1.JobQueryServiceGrpc;
import com.yorel.muxon.grpc.workflow.v1.VMWorkflowServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC client stubs for the three workflow services hosted by services/orchestrator. All three
 * stubs share a single managed channel (named {@code orchestrator}; configure via {@code
 * spring.grpc.client.channels.orchestrator.*}).
 *
 * <p>Example {@code application.yaml}:
 *
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
