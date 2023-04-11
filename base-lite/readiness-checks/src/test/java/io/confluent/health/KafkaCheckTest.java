package io.confluent.health;

import org.apache.kafka.clients.CommonClientConfigs;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

import static org.junit.Assert.*;

public class KafkaCheckTest {

    private static KafkaContainer kafka;
    private static KafkaCheck kafkaCheck;
    private Properties properties;

    @BeforeClass
    public static void setupTests() {
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1")).withKraft();
        kafka.start();
        kafkaCheck = new KafkaCheck();
    }

    @AfterClass
    public static void cleanup() {
        kafka.close();
    }

    @Before
    public void setProperties() {
        properties = new Properties();
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    }

    @Test(timeout = 120000)
    public void kafkaReadyTest() {
        boolean actualVal = kafkaCheck.doCheck(1, 10000, properties);
        assertTrue(actualVal);
    }

    @Test(timeout = 120000)
    public void kafkaReadyWithLessBrokersTest() {
        boolean actualVal = kafkaCheck.doCheck(3, 10000, properties);
        assertFalse(actualVal);
    }

    @Test(timeout = 120000)
    public void kafkaReadyWithInvalidBrokersTest() {
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:2020");

        boolean actualVal = kafkaCheck.doCheck(1, 10000, properties);
        assertFalse(actualVal);
    }
}

