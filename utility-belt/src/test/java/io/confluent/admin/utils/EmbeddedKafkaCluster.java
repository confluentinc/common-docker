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

import kafka.security.JaasTestUtils;
import kafka.security.minikdc.MiniKdc;
import kafka.utils.CoreUtils;
import kafka.utils.TestUtils;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.common.test.KafkaClusterTestKit;
import org.apache.kafka.common.test.TestKitNodes;
import org.apache.kafka.common.KafkaException;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Option$;
import scala.collection.JavaConverters;

import javax.security.auth.login.Configuration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is based on code from
 * src/test/java/io/confluent/support/metrics/common/kafka/EmbeddedKafkaCluster.java
 * at
 * https://github.com/confluentinc/support-metrics-common/support-metrics-common/
 *
 * Starts an embedded Kafka cluster including a backing ZooKeeper ensemble. It adds support for
 * 1. Kafka with SASL_SSL security
 * <p>
 * This class should be used for unit/integration testing only.
 */
public class EmbeddedKafkaCluster {

  private static final Logger log = LoggerFactory.getLogger(EmbeddedKafkaCluster.class);

  private static final Option<SecurityProtocol> INTER_BROKER_SECURITY_PROTOCOL = Option.apply
      (SecurityProtocol.PLAINTEXT);
  private static final boolean ENABLE_CONTROLLED_SHUTDOWN = true;
  private static final boolean ENABLE_DELETE_TOPIC = false;
  private static final boolean ENABLE_PLAINTEXT = true;
  private static final boolean ENABLE_SASL_PLAINTEXT = false;
  private static final int SASL_PLAINTEXT_PORT = 0;
  private static final boolean ENABLE_SSL = false;
  private static final int SSL_PORT = 0;
  private static final int NUM_PARTITIONS = 1;
  private static final short DEFAULT_REPLICATION_FACTOR = 1;
  private static Option<Properties> brokerSaslProperties = Option$.MODULE$.<Properties>empty();
  private static MiniKdc kdc;
  private static File trustStoreFile;
  private static Properties saslProperties;
  private File jaasFilePath = null;
  private Option<File> brokerTrustStoreFile = Option$.MODULE$.<File>empty();
  private boolean enableSASLSSL = false;
  private int numBrokers;
  private boolean isRunning = false;
  private KafkaClusterTestKit cluster;

  public EmbeddedKafkaCluster(int numBrokers) throws Exception {
    this(numBrokers, false);

  }

  public EmbeddedKafkaCluster(int numBrokers, boolean enableSASLSSL)
          throws Exception {
    this(numBrokers, enableSASLSSL, null, null);
  }

  public EmbeddedKafkaCluster(
      int numBrokers, boolean enableSASLSSL,
      String jaasFilePath, String miniKDCDir
  ) throws Exception {
    this.enableSASLSSL = enableSASLSSL;

    if (numBrokers <= 0) {
      throw new IllegalArgumentException("number of servers must be >= 1");
    }

    if (jaasFilePath != null) {
      this.jaasFilePath = new File(jaasFilePath);
    }
    this.numBrokers = numBrokers;

    if (this.enableSASLSSL) {
      File workDir;
      if (miniKDCDir != null) {
        workDir = new File(miniKDCDir);
      } else {
        workDir = Files.createTempDirectory("kdc").toFile();
      }
      Properties kdcConf = MiniKdc.createConfig();
      kdc = new MiniKdc(kdcConf, workDir);
      kdc.start();

      String jaasFile = createJAASFile();

      System.setProperty("java.security.auth.login.config", jaasFile);
      System.setProperty(
          "zookeeper.authProvider.1",
          "org.apache.zookeeper.server.auth.SASLAuthenticationProvider"
      );
      // Uncomment this to debug Kerberos issues.
      // System.setProperty("sun.security.krb5.debug","true");

      trustStoreFile = File.createTempFile("truststore", ".jks");

      saslProperties = new Properties();
      saslProperties.put(SaslConfigs.SASL_MECHANISM, "GSSAPI");
      saslProperties.put(BrokerSecurityConfigs.SASL_ENABLED_MECHANISMS_CONFIG, "GSSAPI");

      this.brokerTrustStoreFile = Option.apply(trustStoreFile);
      this.brokerSaslProperties = Option.apply(saslProperties);
    }

  }

  private String createJAASFile() throws IOException {

    String zkServerPrincipal = "zookeeper/localhost";
    String zkClientPrincipal = "zkclient/localhost";
    String kafkaServerPrincipal = "kafka/localhost";
    String kafkaClientPrincipal = "client/localhost";

    if (jaasFilePath == null) {
      jaasFilePath = new File(Files.createTempDirectory("sasl").toFile(), "jaas.conf");
    }

    FileWriter fwriter = new FileWriter(jaasFilePath);

    String template =
        "" +
        "Server {\n" +
        "   com.sun.security.auth.module.Krb5LoginModule required\n" +
        "   useKeyTab=true\n" +
        "   keyTab=\"$ZK_SERVER_KEYTAB$\"\n" +
        "   storeKey=true\n" +
        "   useTicketCache=false\n" +
        "   principal=\"$ZK_SERVER_PRINCIPAL$@EXAMPLE.COM\";\n" +
        "};\n" +
        "Client {\n" +
        "com.sun.security.auth.module.Krb5LoginModule required\n" +
        "   useKeyTab=true\n" +
        "   keyTab=\"$ZK_CLIENT_KEYTAB$\"\n" +
        "   storeKey=true\n" +
        "   useTicketCache=false\n" +
        "   principal=\"$ZK_CLIENT_PRINCIPAL$@EXAMPLE.COM\";" +
        "};" + "\n" +
        "KafkaServer {\n" +
        "   com.sun.security.auth.module.Krb5LoginModule required\n" +
        "   useKeyTab=true\n" +
        "   keyTab=\"$KAFKA_SERVER_KEYTAB$\"\n" +
        "   storeKey=true\n" +
        "   useTicketCache=false\n" +
        "   serviceName=kafka\n" +
        "   principal=\"$KAFKA_SERVER_PRINCIPAL$@EXAMPLE.COM\";\n" +
        "};\n" +
        "KafkaClient {\n" +
        "com.sun.security.auth.module.Krb5LoginModule required\n" +
        "   useKeyTab=true\n" +
        "   keyTab=\"$KAFKA_CLIENT_KEYTAB$\"\n" +
        "   storeKey=true\n" +
        "   useTicketCache=false\n" +
        "   serviceName=kafka\n" +
        "   principal=\"$KAFKA_CLIENT_PRINCIPAL$@EXAMPLE.COM\";" +
        "};" + "\n";

    String output = template
        .replace("$ZK_SERVER_KEYTAB$", createKeytab(zkServerPrincipal))
        .replace("$ZK_SERVER_PRINCIPAL$", zkServerPrincipal)
        .replace("$ZK_CLIENT_KEYTAB$", createKeytab(zkClientPrincipal))
        .replace("$ZK_CLIENT_PRINCIPAL$", zkClientPrincipal)
        .replace("$KAFKA_SERVER_KEYTAB$", createKeytab(kafkaServerPrincipal))
        .replace("$KAFKA_SERVER_PRINCIPAL$", kafkaServerPrincipal)
        .replace("$KAFKA_CLIENT_KEYTAB$", createKeytab(kafkaClientPrincipal))
        .replace("$KAFKA_CLIENT_PRINCIPAL$", kafkaClientPrincipal);

    log.debug("JAAS Config: " + output);

    fwriter.write(output);

    fwriter.close();
    return jaasFilePath.getAbsolutePath();

  }

  private String createKeytab(String principal) throws IOException {

    File keytabFile = TestUtils.tempFile();

    List<String> principals = new ArrayList<>();
    principals.add(principal);
    kdc.createPrincipal(
        keytabFile,
            principals
    );

    log.debug("Keytab file for " + principal + " : " + keytabFile.getAbsolutePath());
    return keytabFile.getAbsolutePath();
  }

  public static void main(String... args) throws Exception {

    if (args.length != 5) {
      System.err.println(
          "Usage : <command> <num_kafka_brokers> " +
          "<sasl_ssl_enabled> <client properties path> <jaas_file> " +
          "<minikdc_working_dir>"
      );
      System.exit(1);
    }

    int numBrokers = Integer.parseInt(args[0]);
    boolean isSASLSSLEnabled = Boolean.parseBoolean(args[1]);
    String clientPropsPath = args[2];
    String jaasConfigPath = args[3];
    String miniKDCDir = args[4];

    System.out.println(
        "Starting a " + numBrokers + " node Kafka cluster"
    );
    if (isSASLSSLEnabled) {
      System.out.println("SASL_SSL is enabled. jaas.conf=" + jaasConfigPath);
      System.out.println("SASL_SSL is enabled. krb.conf=" + miniKDCDir + "/krb.conf");
    }
    final EmbeddedKafkaCluster kafka = new EmbeddedKafkaCluster(
        numBrokers,
        isSASLSSLEnabled,
        jaasConfigPath,
        miniKDCDir
    );

    System.out.println("Writing client properties to " + clientPropsPath);
    Properties props = kafka.getClientSecurityConfig();
    Password trustStorePassword = (Password) props.get("ssl.truststore.password");
    props.put("ssl.truststore.password", trustStorePassword.value());
    props.put("ssl.enabled.protocols", "TLSv1.2");
    props.store(new FileOutputStream(clientPropsPath), null);

    kafka.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        kafka.shutdown();
      }
    });

  }

  public Properties getClientSecurityConfig() throws Exception {
    if (enableSASLSSL) {
      Properties clientSecurityProps = JaasTestUtils.producerSecurityConfigs(
          SecurityProtocol.SASL_SSL,
          Optional.of(trustStoreFile),
          Optional.of(saslProperties)
      );

      return clientSecurityProps;
    } else {
      return new Properties();
    }
  }

  public void start() throws IOException {
    try {
      final KafkaClusterTestKit.Builder clusterBuilder = new KafkaClusterTestKit.Builder(
              new TestKitNodes.Builder()
                      .setCombined(true)
                      .setNumBrokerNodes(numBrokers)
                      .setPerServerProperties(Map.of(0,
                              Maps.newHashMap(Maps.fromProperties(config))))
                      .setNumControllerNodes(1)
                      .build()
      );

      cluster = clusterBuilder.build();
      //cluster.nonFatalFaultHandler().setIgnore(true);

      cluster.format();
      cluster.startup();
      cluster.waitForReadyBrokers();
    } catch (final Exception e) {
      throw new KafkaException("Failed to create test Kafka cluster", e);
    }
    log.debug("Startup of embedded Kafka broker at {} completed ...", brokerList());
  }

  public void shutdown() {
    log.debug("Shutting down embedded Kafka broker at {} ...", brokerList());
    try {
      cluster.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      log.debug("Deleting logs.dir at {} ...", logDir());
      try {
        Files.delete(Paths.get(logDir()));
      } catch (final IOException e) {
        log.error("Failed to delete log dir {}", logDir(), e);
      }
      log.debug("Shutdown of embedded Kafka broker at {} completed ...", brokerList());
    }
    isRunning = false;
  }

/**
  private void startBroker(int brokerId, String zkConnectString) throws IOException {
    if (brokerId < 0) {
      throw new IllegalArgumentException("broker id must not be negative");
    }

    Properties props = TestUtils
        .createBrokerConfig(
            brokerId,
            ENABLE_CONTROLLED_SHUTDOWN,
            ENABLE_DELETE_TOPIC,
            0,
            INTER_BROKER_SECURITY_PROTOCOL,
            this.brokerTrustStoreFile,
            this.brokerSaslProperties,
            ENABLE_PLAINTEXT,
            ENABLE_SASL_PLAINTEXT,
            SASL_PLAINTEXT_PORT,
            ENABLE_SSL,
            SSL_PORT,
            this.enableSASLSSL,
            0,
            Option.<String>empty(),
            1,
            false,
            NUM_PARTITIONS,
            DEFAULT_REPLICATION_FACTOR,
            false
        );

    KafkaRaftServer broker = TestUtils.createServer(KafkaConfig.fromProps(props), Time.SYSTEM);
    brokersById.put(brokerId, broker);
  }
 **/
/**
  private void stopBroker(int brokerId) {
    if (brokersById.containsKey(brokerId)) {
      KafkaRaftServer broker = brokersById.get(brokerId);
      broker.shutdown();
      broker.awaitShutdown();
      CoreUtils.delete(broker.config().logDirs());
      brokersById.remove(brokerId);
    }
  }
 **/

  public void setJaasFilePath(File jaasFilePath) {
    this.jaasFilePath = jaasFilePath;
  }

  public String getBootstrapBroker(SecurityProtocol securityProtocol) {
    return cluster.bootstrapServers();
  }

  public boolean isRunning() {
    return isRunning;
  }
  String brokerList() {
    return cluster.bootstrapServers();
  }

}
