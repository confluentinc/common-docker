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

import org.apache.kafka.common.config.ConfigException;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class KafkaReadyCommandTest {

  // --- stripConfigProviders ---

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
  }

  @Test
  public void stripConfigProviders_noopWhenNoConfigProviders() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("security.protocol", "PLAINTEXT");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).hasSize(2);
    assertThat(props).containsEntry("bootstrap.servers", "localhost:9092");
  }

  @Test
  public void stripConfigProviders_handlesMultipleProviders() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager,vault");
    props.put("config.providers.secretmanager.class", "com.example.SecretsProvider");
    props.put("config.providers.vault.class", "com.example.VaultProvider");
    props.put("config.providers.vault.param.address", "https://vault:8200");
    props.put("ssl.truststore.location", "/etc/ssl/truststore.jks");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).containsOnlyKeys("bootstrap.servers", "ssl.truststore.location");
  }

  @Test
  public void stripConfigProviders_doesNotRemoveSimilarKeys() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.Provider");
    props.put("some.config.providers.unrelated", "value");

    KafkaReadyCommand.stripConfigProviders(props);

    assertThat(props).containsOnlyKeys("bootstrap.servers", "some.config.providers.unrelated");
  }

  // --- hasConfigProviders ---

  @Test
  public void hasConfigProviders_trueWhenPresent() {
    Map<String, String> props = new HashMap<>();
    props.put("config.providers", "secretmanager");

    assertThat(KafkaReadyCommand.hasConfigProviders(props)).isTrue();
  }

  @Test
  public void hasConfigProviders_falseWhenAbsent() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");

    assertThat(KafkaReadyCommand.hasConfigProviders(props)).isFalse();
  }

  // --- isConfigProviderLoadFailure ---

  @Test
  public void isConfigProviderLoadFailure_trueForClassNotFoundException() {
    ConfigException e = new ConfigException(
        "Invalid value com.example.Provider for configuration "
        + "config.providers.secretmanager.class: Could not load class");
    e.initCause(new ClassNotFoundException("com.example.Provider"));

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isTrue();
  }

  @Test
  public void isConfigProviderLoadFailure_trueForNoClassDefFoundError() {
    ConfigException e = new ConfigException(
        "Invalid value com.example.Provider for configuration "
        + "config.providers.secretmanager.class: Could not load class");
    e.initCause(new NoClassDefFoundError("software/amazon/awssdk/core/SdkClient"));

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isTrue();
  }

  @Test
  public void isConfigProviderLoadFailure_trueForNestedClassNotFound() {
    ClassNotFoundException cnfe = new ClassNotFoundException("com.example.Provider");
    RuntimeException wrapper = new RuntimeException("wrapper", cnfe);
    ConfigException e = new ConfigException(
        "config.providers.vault.class: Could not load");
    e.initCause(wrapper);

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isTrue();
  }

  @Test
  public void isConfigProviderLoadFailure_trueForMessageFallback() {
    // No ClassNotFoundException in cause chain, but message matches
    ConfigException e = new ConfigException(
        "Invalid value com.example.Provider "
        + "for configuration config.providers.secretmanager.class: "
        + "Could not load config provider class or one of its dependencies");

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isTrue();
  }

  @Test
  public void isConfigProviderLoadFailure_falseForOtherConfigException() {
    ConfigException e = new ConfigException("Unknown configuration key: bad.key");

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isFalse();
  }

  @Test
  public void isConfigProviderLoadFailure_falseForSaslClassNotFound() {
    // CNFE for a SASL handler class should NOT be treated as a config provider failure
    ConfigException e = new ConfigException(
        "Invalid value com.example.SaslHandler for configuration "
        + "sasl.client.callback.handler.class: Could not load class");
    e.initCause(new ClassNotFoundException("com.example.SaslHandler"));

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isFalse();
  }

  @Test
  public void isConfigProviderLoadFailure_falseForSslClassNotFound() {
    // CNFE for an SSL engine factory should NOT match
    ConfigException e = new ConfigException(
        "Invalid value com.example.SslFactory for configuration "
        + "ssl.engine.factory.class: Could not load class");
    e.initCause(new ClassNotFoundException("com.example.SslFactory"));

    assertThat(KafkaReadyCommand.isConfigProviderLoadFailure(e)).isFalse();
  }

  // --- findUnresolvedConfigProviderVars ---

  @Test
  public void findUnresolvedVars_detectsProviderReferences() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("ssl.keystore.password", "${secretmanager:my-secret:keystore-password}");
    props.put("sasl.jaas.config", "${vault:secret/kafka:jaas-config}");
    props.put("security.protocol", "SASL_SSL");

    List<String> unresolved = KafkaReadyCommand.findUnresolvedConfigProviderVars(props);

    assertThat(unresolved).hasSize(2);
    assertThat(unresolved).contains("ssl.keystore.password", "sasl.jaas.config");
  }

  @Test
  public void findUnresolvedVars_ignoresNonProviderPlaceholders() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    // Shell-style ${ENV_VAR} without colons should NOT be matched
    props.put("some.prop", "${SOME_ENV_VAR}");
    // But provider-style ${provider:path} should be matched
    props.put("ssl.keystore.password", "${secretmanager:my-secret:password}");

    List<String> unresolved = KafkaReadyCommand.findUnresolvedConfigProviderVars(props);

    assertThat(unresolved).hasSize(1);
    assertThat(unresolved).contains("ssl.keystore.password");
  }

  @Test
  public void findUnresolvedVars_emptyWhenAllLiteral() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("ssl.keystore.password", "my-literal-password");
    props.put("security.protocol", "SSL");

    List<String> unresolved = KafkaReadyCommand.findUnresolvedConfigProviderVars(props);

    assertThat(unresolved).isEmpty();
  }

  @Test
  public void findUnresolvedVars_handlesEmptyMap() {
    List<String> unresolved = KafkaReadyCommand.findUnresolvedConfigProviderVars(new HashMap<>());

    assertThat(unresolved).isEmpty();
  }

  @Test
  public void findUnresolvedVars_ignoresNonClientKeys() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    // Non-client key with provider reference should be ignored
    props.put("connector.password", "${secretmanager:secret:connector-pw}");
    props.put("group.id", "${secretmanager:secret:group}");
    // Client key with provider reference should be detected
    props.put("ssl.keystore.password", "${secretmanager:secret:keystore-pw}");

    List<String> unresolved = KafkaReadyCommand.findUnresolvedConfigProviderVars(props);

    assertThat(unresolved).hasSize(1);
    assertThat(unresolved).contains("ssl.keystore.password");
  }

  // --- checkKafkaReadyWithConfigProviderResilience orchestration ---

  @Test
  public void resilience_noConfigProviders_callsCheckerOnce() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    AtomicInteger callCount = new AtomicInteger(0);
    KafkaReadyCommand.KafkaReadyChecker checker = (config, brokers, timeout) -> {
      callCount.incrementAndGet();
      return true;
    };

    boolean result = KafkaReadyCommand.checkKafkaReadyWithConfigProviderResilience(
        props, 1, 5000, checker);

    assertThat(result).isTrue();
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  public void resilience_configProvidersLoadable_succeedsFirstTry() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.Provider");
    AtomicInteger callCount = new AtomicInteger(0);
    KafkaReadyCommand.KafkaReadyChecker checker = (config, brokers, timeout) -> {
      callCount.incrementAndGet();
      return true;
    };

    boolean result = KafkaReadyCommand.checkKafkaReadyWithConfigProviderResilience(
        props, 1, 5000, checker);

    assertThat(result).isTrue();
    assertThat(callCount.get()).isEqualTo(1);
    // config.providers still present since first try succeeded
    assertThat(props).containsKey("config.providers");
  }

  @Test
  public void resilience_classNotFound_literalProps_stripsAndRetries() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("security.protocol", "SASL_SSL");
    props.put("sasl.mechanism", "PLAIN");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.MissingProvider");
    AtomicInteger callCount = new AtomicInteger(0);
    KafkaReadyCommand.KafkaReadyChecker checker = (config, brokers, timeout) -> {
      int call = callCount.incrementAndGet();
      if (call == 1) {
        ConfigException ex = new ConfigException(
            "Invalid value com.example.MissingProvider for configuration "
            + "config.providers.secretmanager.class: Could not load");
        ex.initCause(new ClassNotFoundException("com.example.MissingProvider"));
        throw ex;
      }
      return true;
    };

    boolean result = KafkaReadyCommand.checkKafkaReadyWithConfigProviderResilience(
        props, 1, 5000, checker);

    assertThat(result).isTrue();
    assertThat(callCount.get()).isEqualTo(2);
    // caller's map should not be mutated
    assertThat(props).containsKey("config.providers");
    assertThat(props).containsKey("config.providers.secretmanager.class");
    assertThat(props).containsKey("bootstrap.servers");
  }

  @Test
  public void resilience_classNotFound_unresolvedVars_skipsWithWarning() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("ssl.keystore.password", "${secretmanager:secret:password}");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.MissingProvider");
    AtomicInteger callCount = new AtomicInteger(0);
    KafkaReadyCommand.KafkaReadyChecker checker = (config, brokers, timeout) -> {
      callCount.incrementAndGet();
      ConfigException ex = new ConfigException(
          "Invalid value com.example.MissingProvider for configuration "
          + "config.providers.secretmanager.class: Could not load");
      ex.initCause(new ClassNotFoundException("com.example.MissingProvider"));
      throw ex;
    };

    boolean result = KafkaReadyCommand.checkKafkaReadyWithConfigProviderResilience(
        props, 1, 5000, checker);

    // Should skip (return true) without retrying
    assertThat(result).isTrue();
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  public void resilience_nonProviderConfigException_rethrows() {
    Map<String, String> props = new HashMap<>();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("config.providers", "secretmanager");
    props.put("config.providers.secretmanager.class", "com.example.Provider");
    KafkaReadyCommand.KafkaReadyChecker checker = (config, brokers, timeout) -> {
      throw new ConfigException("Unknown configuration key: bad.key");
    };

    try {
      KafkaReadyCommand.checkKafkaReadyWithConfigProviderResilience(
          props, 1, 5000, checker);
      fail("Expected ConfigException to be rethrown");
    } catch (ConfigException e) {
      assertThat(e.getMessage()).contains("bad.key");
    }
  }
}
