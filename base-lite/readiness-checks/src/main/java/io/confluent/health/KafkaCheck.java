package io.confluent.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.common.Node;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Properties;

/**
 * Check whether a Kafka cluster has enough online brokers by performing a describeCluster API call and counting the number of nodes.
 */
public class KafkaCheck {

    public static final int BROKER_METADATA_REQUEST_BACKOFF_MS = 1000;

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // this is okay, we just wake up early
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks if the kafka cluster is accepting client requests and
     * has at least minBrokerCount brokers.
     *
     * @param minBrokerCount Expected no of brokers
     * @param timeoutMs timeoutMs in milliseconds
     * @return true is the cluster is ready, false otherwise.
     */
    boolean doCheck(int minBrokerCount, long timeoutMs, Properties adminConfig) {
        final long begin = System.currentTimeMillis();
        final AdminClient client = AdminClient.create(adminConfig);
        final Object bootstrapServers = adminConfig.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);

        long remainingWaitMs = timeoutMs;
        Collection<Node> brokers = null;
        while (remainingWaitMs > 0) {
            System.out.printf("Querying Kafka for metadata (bootstrap: %s). ", bootstrapServers);
            // describeCluster does not wait for all brokers to be ready before returning the brokers.
            // So, wait until expected brokers are present or the timeout expires.
            try {
                brokers = client.describeCluster(new DescribeClusterOptions().timeoutMs(
                        (int) Math.min(Integer.MAX_VALUE, remainingWaitMs))).nodes().get();
                System.out.printf("Broker list: %s%n", (brokers != null ? brokers : "[]"));
                if ((brokers != null) && (brokers.size() >= minBrokerCount)) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println("Error while getting broker list: " + e.getMessage());
                // Swallow exceptions because we want to retry until timeoutMs expires.
            }

            sleep(Math.min(BROKER_METADATA_REQUEST_BACKOFF_MS, remainingWaitMs));
            final long elapsed = System.currentTimeMillis() - begin;
            remainingWaitMs = timeoutMs - elapsed;
        }

        System.err.printf("Expected %d brokers but found only %d after %d ms%n.",
                minBrokerCount,
                brokers == null ? 0 : brokers.size(),
                timeoutMs
        );
        return false;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage <minBrokers> <timeoutMs> <pathToProperties>" );
            System.exit(1);
        }
        final Properties adminProperties = new Properties();
        try {
            adminProperties.load(new FileInputStream(args[2]));
            final boolean enoughBrokerAvailable = new KafkaCheck().doCheck(Integer.parseInt(args[0]), Long.parseLong(args[1]), adminProperties);
            if (!enoughBrokerAvailable) {
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error while checking kafka brokers: " + e.getMessage());
            System.exit(1);
        }
    }
}

