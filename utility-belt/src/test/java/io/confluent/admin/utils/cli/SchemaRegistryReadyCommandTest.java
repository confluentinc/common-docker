/*
 * Copyright 2017 Confluent Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.admin.utils.cli;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SchemaRegistryReadyCommandTest {

  @Test
  public void testIsSchemaRegistryReadyWithInvalidHost() {
    // Test with an invalid host that should not be reachable
    boolean result = SchemaRegistryReadyCommand.isSchemaRegistryReady("invalid-host", 8081, 5);
    assertThat(result).isFalse();
  }

  @Test
  public void testIsSchemaRegistryReadyWithInvalidPort() {
    // Test with an invalid port that should not be reachable
    boolean result = SchemaRegistryReadyCommand.isSchemaRegistryReady("localhost", 99999, 5);
    assertThat(result).isFalse();
  }

  @Test
  public void testIsSchemaRegistryReadyWithShortTimeout() {
    // Test with a very short timeout
    boolean result = SchemaRegistryReadyCommand.isSchemaRegistryReady("localhost", 8081, 1);
    // Should return false quickly for unreachable service
    assertThat(result).isFalse();
  }
} 