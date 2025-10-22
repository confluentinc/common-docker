/**
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

import javax.security.auth.login.Configuration;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Skipping all tests in this class as minikdc is not allowed to update the krb5.conf in java 17")
public class ClusterStatusSASLTest {

  private static final Logger log = LogManager.getLogger(ClusterStatusSASLTest.class);

  private static EmbeddedKafkaCluster kafka;


  @BeforeClass
  public static void setup() throws Exception {
    Configuration.setConfiguration(null);

    kafka = new EmbeddedKafkaCluster(3, true);
    kafka.start();
  }


  @AfterClass
  public static void tearDown() throws Exception {
    kafka.shutdown();
  }

  @Test(timeout = 120000)
  public void isKafkaReadyWithSASLAndSSL() throws Exception {
    Properties clientSecurityProps = kafka.getClientSecurityConfig();

    Map<String, String> config = Utils.propsToStringMap(clientSecurityProps);
    config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapBrokers
        (SecurityProtocol.SASL_SSL));

    // Set password and enabled protocol as the Utils.propsToStringMap just returns toString()
    // representations and these properties don't have a valid representation.
    Password trustStorePassword = (Password) clientSecurityProps.get("ssl.truststore.password");
    config.put("ssl.truststore.password", trustStorePassword.value());
    config.put("ssl.enabled.protocols", "TLSv1.2");

    assertThat(ClusterStatus.isKafkaReady(config, 3, 10000)).isTrue();
  }
}
