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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.confluent.admin.utils.ClusterStatus;

import static net.sourceforge.argparse4j.impl.Arguments.store;

/**
 * This command checks if the kafka cluster has the expected number of brokers and is ready to
 * accept
 * requests.
 * where:
 * config                 : path to properties with client config.
 * min-expected-brokers   : minimum brokers to wait for.
 * timeout                : timeout in ms for all operations. This includes looking up metadata in
 * Zookeeper or fetching metadata for the brokers.
 * (bootstrap-brokers
 * or
 * zookeeper-connect)     : Either a bootstrap broker list or zookeeper connect string
 * security-protocol      : Security protocol to use to connect to the broker.
 */
public class KafkaReadyCommand {

  private static final Logger log = LogManager.getLogger(KafkaReadyCommand.class);
  public static final String KAFKA_READY = "kafka-ready";
  private static final String CONFIG_PROVIDERS_PREFIX = "config.providers";

  @FunctionalInterface
  interface KafkaReadyChecker {
    boolean isReady(Map<String, String> config, int minBrokerCount, int timeoutMs);
  }

  // Matches Kafka config provider variable syntax: ${provider-name:path[:key]}
  // Requires at least one colon separator to distinguish from other ${...} placeholders.
  private static final Pattern CONFIG_PROVIDER_VAR_PATTERN =
      Pattern.compile("\\$\\{[^:}]+:[^}]+}");

  // Property key prefixes that affect Kafka client connectivity. Only these are checked
  // for unresolved config provider variable references when deciding whether to skip
  // the kafka-ready check. Non-client keys (e.g. connector configs) are irrelevant
  // since AdminClient ignores them.
  private static final String[] KAFKA_CLIENT_KEY_PREFIXES = {
      "security.", "sasl.", "ssl.", "bootstrap.servers"
  };

  private static ArgumentParser createArgsParser() {
    ArgumentParser kafkaReady = ArgumentParsers
        .newArgumentParser(KAFKA_READY)
        .defaultHelp(true)
        .description("Check if Kafka is ready.");

    kafkaReady.addArgument("min-expected-brokers")
        .action(store())
        .required(true)
        .type(Integer.class)
        .metavar("MIN_EXPECTED_BROKERS")
        .help("Minimum number of brokers to wait for.");

    kafkaReady.addArgument("timeout")
        .action(store())
        .required(true)
        .type(Integer.class)
        .metavar("TIMEOUT_IN_MS")
        .help("Time (in ms) to wait for service to be ready.");

    kafkaReady.addArgument("--config", "-c")
        .action(store())
        .type(String.class)
        .metavar("CONFIG")
        .help("List of bootstrap brokers.");

    MutuallyExclusiveGroup kafkaOrZK = kafkaReady.addMutuallyExclusiveGroup();
    kafkaOrZK.addArgument("--bootstrap-servers", "-b")
        .action(store())
        .type(String.class)
        .metavar("BOOTSTRAP_SERVERS")
        .help("List of bootstrap brokers.");

    kafkaOrZK.addArgument("--zookeeper-connect", "-z")
        .action(store())
        .type(String.class)
        .metavar("ZOOKEEPER_CONNECT")
        .help("Zookeeper connect string.");

    kafkaReady.addArgument("--security-protocol", "-s")
        .action(store())
        .type(String.class)
        .metavar("SECURITY_PROTOCOL")
        .setDefault("PLAINTEXT")
        .help("Which endpoint to connect to ? ");

    return kafkaReady;
  }

  public static void main(String[] args) {
    ArgumentParser parser = createArgsParser();
    boolean success = false;
    try {
      Namespace res = parser.parseArgs(args);
      log.debug("Arguments {}. ", res);

      Map<String, String> workerProps = new HashMap<>();

      if (res.getString("config") == null
          && !(res.getString("security_protocol").equals("PLAINTEXT"))) {
        log.error("config is required for all protocols except PLAINTEXT");
        success = false;
      } else {
        if (res.getString("config") != null) {
          workerProps = Utils.propsToStringMap(Utils.loadProps(res.getString("config")));
        }
        if (res.getString("bootstrap_servers") != null) {
          workerProps.put(
              CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
              res.getString("bootstrap_servers")
          );
        } else {
          log.error("Bootstrap servers should be provided through config or bootstrap_servers");
          throw new RuntimeException(
              "Bootstrap servers should be provided through config or bootstrap_servers"
          );
        }
        success = checkKafkaReadyWithConfigProviderResilience(
            workerProps,
            res.getInt("min_expected_brokers"),
            res.getInt("timeout")
        );
      }

    } catch (ArgumentParserException e) {
      if (args.length == 0) {
        parser.printHelp();
        success = true;
      } else {
        parser.handleError(e);
        success = false;
      }
    } catch (Exception e) {
      log.error("Error while running kafka-ready.", e);
      success = false;
    }

    if (success) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  /**
   * Attempts the kafka-ready check, handling config provider class loading failures gracefully.
   *
   * <p>Strategy:
   * 0. If no config.providers entries are present, run the check directly (fast path).
   * 1. Try with the full config (config providers may be loadable if on the classpath).
   * 2. If config provider class loading fails (ConfigException wrapping ClassNotFoundException),
   *    strip config.providers entries from a copy and check for unresolved variable references
   *    in Kafka client connectivity properties (security.*, sasl.*, ssl.*).
   * 3. If client connectivity properties contain unresolved ${provider:path:key} references,
   *    skip the check with a warning - the Connect worker will verify connectivity on startup.
   * 4. Otherwise, retry the check with the stripped copy (without config.providers).
   *
   * <p>The caller's workerProps map is never mutated.
   */
  static boolean checkKafkaReadyWithConfigProviderResilience(
      Map<String, String> workerProps,
      int minBrokerCount,
      int timeoutMs
  ) {
    return checkKafkaReadyWithConfigProviderResilience(
        workerProps, minBrokerCount, timeoutMs, ClusterStatus::isKafkaReady);
  }

  static boolean checkKafkaReadyWithConfigProviderResilience(
      Map<String, String> workerProps,
      int minBrokerCount,
      int timeoutMs,
      KafkaReadyChecker checker
  ) {
    if (!hasConfigProviders(workerProps)) {
      return checker.isReady(workerProps, minBrokerCount, timeoutMs);
    }

    try {
      return checker.isReady(workerProps, minBrokerCount, timeoutMs);
    } catch (ConfigException e) {
      if (!isConfigProviderLoadFailure(e)) {
        throw e;
      }
      log.warn("Config provider class could not be loaded during kafka-ready check: {}",
          e.getMessage(), e);

      Map<String, String> strippedProps = new HashMap<>(workerProps);
      stripConfigProviders(strippedProps);

      List<String> unresolvedKeys = findUnresolvedConfigProviderVars(strippedProps);
      if (!unresolvedKeys.isEmpty()) {
        log.warn("Skipping kafka-ready check - the following properties contain unresolved "
            + "config provider variable references that cannot be resolved without the "
            + "config provider plugin: {}. The Connect worker will verify broker connectivity "
            + "on startup.", unresolvedKeys);
        return true;
      }

      log.warn("Retrying kafka-ready check without config provider properties.");
      return checker.isReady(strippedProps, minBrokerCount, timeoutMs);
    }
  }

  /**
   * Returns true if the properties map contains config.providers entries.
   */
  static boolean hasConfigProviders(Map<String, String> props) {
    return props.containsKey(CONFIG_PROVIDERS_PREFIX);
  }

  /**
   * Returns true if the exception is a ConfigException specifically caused by a config
   * provider class loading failure. Requires both:
   * - The exception message references "config.providers" (to distinguish from SASL/SSL
   *   handler class loading failures which also throw ConfigException with CNFE)
   * - Either a ClassNotFoundException/NoClassDefFoundError in the cause chain, or
   *   "Could not load" in the message (fallback for truncated cause chains)
   */
  static boolean isConfigProviderLoadFailure(ConfigException e) {
    String msg = e.getMessage();
    boolean mentionsConfigProviders = msg != null && msg.contains("config.providers");
    if (!mentionsConfigProviders) {
      return false;
    }

    Throwable cause = e.getCause();
    while (cause != null) {
      if (cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError) {
        return true;
      }
      cause = cause.getCause();
    }
    // Fallback: message-only check when cause chain is truncated
    return msg.contains("Could not load");
  }

  /**
   * Removes config.providers entries from the properties map.
   */
  static void stripConfigProviders(Map<String, String> props) {
    List<String> removed = new java.util.ArrayList<>();
    Iterator<Map.Entry<String, String>> it = props.entrySet().iterator();
    while (it.hasNext()) {
      String key = it.next().getKey();
      if (key.equals(CONFIG_PROVIDERS_PREFIX) || key.startsWith(CONFIG_PROVIDERS_PREFIX + ".")) {
        removed.add(key);
        it.remove();
      }
    }
    if (!removed.isEmpty()) {
      log.warn("Removed {} config provider properties from kafka-ready config: {}",
          removed.size(), removed);
    }
  }

  /**
   * Returns Kafka client connectivity property keys whose values contain unresolved
   * config provider variable references (e.g. ${secretmanager:path:key}). Only checks
   * keys that affect broker connectivity (security.*, sasl.*, ssl.*, bootstrap.servers)
   * since non-client keys are ignored by AdminClient and won't cause failures.
   */
  static List<String> findUnresolvedConfigProviderVars(Map<String, String> props) {
    return props.entrySet().stream()
        .filter(e -> isKafkaClientKey(e.getKey()))
        .filter(e -> CONFIG_PROVIDER_VAR_PATTERN.matcher(e.getValue()).find())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
  }

  private static boolean isKafkaClientKey(String key) {
    for (String prefix : KAFKA_CLIENT_KEY_PREFIXES) {
      if (key.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
