/*
 * Copyright 2017 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.admin.utils.cli;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaReadyCommandTest {

  @Test
  public void stripConfigProviders_removesAllConfigProviderEntries() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("security.protocol", "SASL_SSL");
    props.put("sasl.mechanism", "PLAIN");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class",
        "io.confluent.csid.config.provider.aws.SecretsManagerConfigProvider");
    props.put("config.providers.secretmanager.param.aws.region", "us-east-1");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).containsOnlyKeys(
        "bootstrap.servers", "security.protocol", "sasl.mechanism");
    assertThat(props).doesNotContainKey("config.providers");
    assertThat(props).doesNotContainKey("config.providers.secretmanager.class");
    assertThat(props).doesNotContainKey("config.providers.secretmanager.param.aws.region");
  }

  @Test
  public void stripConfigProviders_noopWhenNoConfigProviders() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("security.protocol", "PLAINTEXT");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).hasSize(2);
    assertThat(props).containsEntry("bootstrap.servers", "localhost:9092");
    assertThat(props).containsEntry("security.protocol", "PLAINTEXT");
  }

  @Test
  public void stripConfigProviders_handlesMultipleProviders() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager,vault");
    props.put("config.providers.secretmanager.class", "com.example.SecretsProvider");
    props.put("config.providers.secretmanager.param.region", "us-east-1");
    props.put("config.providers.vault.class", "com.example.VaultProvider");
    props.put("config.providers.vault.param.address", "https://vault:8200");
    props.put("ssl.truststore.location", "/etc/ssl/truststore.jks");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).containsOnlyKeys("bootstrap.servers", "ssl.truststore.location");
  }

  @Test
  public void stripConfigProviders_handlesEmptyMap() {
    Map<String, String> props = new HashMap<>();

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).isEmpty();
  }

  @Test
  public void stripConfigProviders_doesNotRemoveSimilarKeys() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.Provider");
    // This key should NOT be removed — it doesn't match the prefix
    props.put("some.config.providers.unrelated", "value");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).containsOnlyKeys("bootstrap.servers", "some.config.providers.unrelated");
  }
}
