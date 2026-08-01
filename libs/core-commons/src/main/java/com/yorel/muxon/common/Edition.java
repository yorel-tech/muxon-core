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
package com.yorel.muxon.common;

/**
 * Product edition for the Muxon backend.
 *
 * <p>This enum is used on the backend side; the value is typically serialized to a lowercase string
 * for the UI (e.g. "core", "enterprise").
 */
public enum Edition {
  CORE,
  ENTERPRISE
}
