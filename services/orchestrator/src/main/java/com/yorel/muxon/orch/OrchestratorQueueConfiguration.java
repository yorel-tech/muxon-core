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
package com.yorel.muxon.orch;

/**
 * This class previously defined database-backed CommandQueue and EventPublisher beans that are now
 * provided centrally by {@link com.yorel.muxon.db.queue.QueueDbConfiguration}.
 *
 * <p>It is kept only as a placeholder to avoid breaking references; all queue bean definitions live
 * in the shared core-persistence module.
 */
public class OrchestratorQueueConfiguration {}
