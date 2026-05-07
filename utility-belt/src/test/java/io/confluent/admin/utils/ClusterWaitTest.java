/**
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
package io.confluent.admin.utils;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ClusterWaitTest {

  private static final long WAIT_TIMEOUT_MS = 60_000;
  private static final long WAIT_POLL_INTERVAL_MS = 100;

  private static void waitForCondition(BooleanSupplier condition, String conditionDetails)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > deadline) {
        throw new AssertionError("Condition not met: " + conditionDetails);
      }
      Thread.sleep(WAIT_POLL_INTERVAL_MS);
    }
  }

  @Test(timeout = 180000)
  public void isKafkaReadyWait() throws Exception {
    final EmbeddedKafkaCluster kafkaWait = new EmbeddedKafkaCluster(3);

    Thread kafkaClusterThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(1000);
          kafkaWait.start();
          while (kafkaWait.isRunning()) {
            Thread.sleep(1000);
          }
        } catch (Exception e) {
          fail("Unexpected exception ", e);
        }
      }
    });

    kafkaClusterThread.start();
    waitForCondition(() -> !kafkaWait.getBootstrapBrokers(SecurityProtocol.PLAINTEXT).isEmpty(),
        "unable to get bootstrap server list.");

    try {
      Map<String, String> config = new HashMap<>();
      config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaWait.getBootstrapBrokers
          (SecurityProtocol.PLAINTEXT));

      assertThat(ClusterStatus.isKafkaReady(config, 3, 20000))
          .isTrue();
    } catch (Exception e) {
      e.printStackTrace();
      fail("Unexpected error." + e.getMessage());
    } finally {
      kafkaWait.shutdown();
    }
    kafkaClusterThread.join(60000);
  }


  @Test(timeout = 180000)
  public void isKafkaReady() throws Exception {
    final EmbeddedKafkaCluster kafkaWait = new EmbeddedKafkaCluster(3);
    Thread kafkaClusterThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(1000);
          kafkaWait.start();
          while (kafkaWait.isRunning()) {
            Thread.sleep(1000);
          }
        } catch (Exception e) {
          fail("Unexpected exception ", e);
        }
      }
    });

    kafkaClusterThread.start();
    waitForCondition(() -> !kafkaWait.getBootstrapBrokers(SecurityProtocol.PLAINTEXT).isEmpty(),
            "unable to get bootstrap server list.");
    try {
      String bootstrap_broker = kafkaWait.getBootstrapBrokers(SecurityProtocol.PLAINTEXT);
      Map<String, String> config = new HashMap<>();
      config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap_broker);

      assertThat(ClusterStatus.isKafkaReady(config, 3, 20000))
          .isTrue();
    } catch (Exception e) {
      fail("Unexpected error." + e.getMessage());
    } finally {
      kafkaWait.shutdown();
    }
    kafkaClusterThread.join(60000);
  }
}
