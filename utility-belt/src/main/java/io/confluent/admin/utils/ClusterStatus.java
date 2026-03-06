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

package io.confluent.admin.utils;

import org.apache.kafka.common.Node;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks status of Kafka cluster.
 */
public class ClusterStatus {

  private static final Logger log = LogManager.getLogger(ClusterStatus.class);
  public static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";
  public static final String BROKERS_IDS_PATH = "/brokers/ids";
  public static final int BROKER_METADATA_REQUEST_BACKOFF_MS = 1000;

  /**
   * Checks if the kafka cluster is accepting client requests and
   * has at least minBrokerCount brokers.
   *
   * @param minBrokerCount Expected no of brokers
   * @param timeoutMs timeoutMs in milliseconds
   * @return true is the cluster is ready, false otherwise.
   */
  public static boolean isKafkaReady(
      Map<String, String> config,
      int minBrokerCount,
      int timeoutMs
  ) {

    // Need to copy because `config` is Map<String, String> and `create` expects Map<String, Object>
    AdminClient adminClient = AdminClient.create(new HashMap<String, Object>(config));

    long begin = System.currentTimeMillis();
    long remainingWaitMs = timeoutMs;
    Collection<Node> brokers = new ArrayList<>();
    while (remainingWaitMs > 0) {

      // describeCluster does not wait for all brokers to be ready before returning the brokers.
      // So, wait until expected brokers are present or the time out expires.
      try {
        brokers = adminClient.describeCluster(new DescribeClusterOptions().timeoutMs(
                (int) Math.min(Integer.MAX_VALUE, remainingWaitMs))).nodes().get();
        log.debug("Broker list: {}", (brokers != null ? brokers : "[]"));
        if ((brokers != null) && (brokers.size() >= minBrokerCount)) {
          return true;
        }
      } catch (Exception e) {
        log.error("Error while getting broker list.", e);
        // Swallow exceptions because we want to retry until timeoutMs expires.
      }

      sleep(Math.min(BROKER_METADATA_REQUEST_BACKOFF_MS, remainingWaitMs));

      log.info(
          "Expected {} brokers but found only {}. Trying to query Kafka for metadata again ...",
          minBrokerCount,
          brokers == null ? 0 : brokers.size()
      );
      long elapsed = System.currentTimeMillis() - begin;
      remainingWaitMs = timeoutMs - elapsed;
    }

    log.error(
        "Expected {} brokers but found only {}. Brokers found {}.",
        minBrokerCount,
        brokers == null ? 0 : brokers.size(),
        brokers != null ? brokers : "[]"
    );

    return false;
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      // this is okay, we just wake up early
      Thread.currentThread().interrupt();
    }
  }
}
