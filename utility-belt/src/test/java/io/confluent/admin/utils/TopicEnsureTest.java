/**
 * Copyright 2018 Confluent Inc.
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

package io.confluent.kafkaensure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.TopicConfig;
import io.confluent.kafkaensure.TopicSpec;

import io.confluent.admin.utils.EmbeddedKafkaCluster;

public class TopicEnsureTest {

  private static final int NUM_BROKERS = 3;
  private static final int NUM_ZK = 3;
  private static final int DEFAULT_PARTITIONS = 2;
  private static final int DEFAULT_REPLICATION_FACTOR = 3;
  private static final Integer TIMEOUT_MS = 20000;

  private static EmbeddedKafkaCluster kafka;
  private static TopicEnsure topicEnsure;

  @Before
  public void setUp() throws IOException {
    kafka = new EmbeddedKafkaCluster(NUM_BROKERS, NUM_ZK);
    kafka.start();

    Properties adminClientProps = new Properties();
    adminClientProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                         kafka.getBootstrapBroker(SecurityProtocol.PLAINTEXT));
    topicEnsure = new TopicEnsure(adminClientProps);
  }

  @After
  public void tearDown() {
    kafka.shutdown();
  }

  @Test
  public void testCreateExistsValidateTopic() throws Exception {
    final TopicSpec spec = simpleTopicSpec("test-topic");
    topicEnsure.createTopic(spec, TIMEOUT_MS);

    assertTrue(topicEnsure.topicExists(spec, TIMEOUT_MS));
    assertTrue(topicEnsure.validateTopic(spec, TIMEOUT_MS));
  }

  @Test
  public void testValidateTopicWithBadConfigEntry() throws Exception {
    final String topicName = "another-topic";
    Map<String, String> topicProps = new HashMap<>();
    topicProps.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");
    TopicSpec spec = new TopicSpec(topicName, 2, 3, topicProps);
    topicEnsure.createTopic(spec, TIMEOUT_MS);

    topicProps.put("incorrect.config", "1");
    assertFalse(topicEnsure.validateTopic(spec, TIMEOUT_MS));
  }

  @Test
  public void testTopicExistsForNonexistentTopic() throws Exception {
    assertFalse(topicEnsure.topicExists(simpleTopicSpec("unknown-topic"), TIMEOUT_MS));
  }

  @Test(expected = Exception.class)
  public void testValidateNonexistentTopic() throws Exception {
    assertFalse(topicEnsure.validateTopic(simpleTopicSpec("unknown-topic"), TIMEOUT_MS));
  }

  @Test
  public void testValidateTopicWithNonMatchingSpec() throws Exception {
    final String topicName = "test-topic";
    topicEnsure.createTopic(simpleTopicSpec(topicName), TIMEOUT_MS);

    TopicSpec spec = new TopicSpec(topicName, 1, DEFAULT_REPLICATION_FACTOR, simpleTopicProps());
    assertFalse(topicEnsure.validateTopic(spec, TIMEOUT_MS));

    TopicSpec spec2 = new TopicSpec(topicName, DEFAULT_PARTITIONS, 1, simpleTopicProps());
    assertFalse(topicEnsure.validateTopic(spec2, TIMEOUT_MS));

    // same spec but with empty config map is ok -- will not validate config entries
    TopicSpec spec3 = new TopicSpec(
        topicName, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR, Collections.emptyMap());
    assertTrue(topicEnsure.validateTopic(spec3, TIMEOUT_MS));
  }

  private static TopicSpec simpleTopicSpec(String topic) {
    return new TopicSpec(topic, DEFAULT_PARTITIONS, DEFAULT_REPLICATION_FACTOR, simpleTopicProps());
  }

  private static Map<String, String> simpleTopicProps() {
    return Collections.singletonMap(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2");
  }
}
