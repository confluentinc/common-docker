package io.confluent.kafkaensure;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TopicSpec {
  public String name;
  public int partitions;
  public int replicationFactor;
  public Map<String, String> config;

  public TopicSpec() {
  }

  public TopicSpec(String name, int partitions, int replicationFactor, Map<String, String> config) {
    this.name = name;
    this.partitions = partitions;
    this.replicationFactor = replicationFactor;
    this.config = config;
  }

  public String name() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int partitions() {
    return partitions;
  }

  public void setPartitions(int partitions) {
    this.partitions = partitions;
  }

  public int replicationFactor() {
    return replicationFactor;
  }

  public void setReplicationFactor(int replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  public Map<String, String> config() {
    return config;
  }

  public void setConfig(Map<String, String> config) {
    this.config = config;
  }

  public void putInConfig(String key, String value) {
    this.config.put(key, value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicSpec topicSpec = (TopicSpec) o;
    return partitions == topicSpec.partitions &&
        replicationFactor == topicSpec.replicationFactor &&
        Objects.equals(name, topicSpec.name) &&
        Objects.equals(config, topicSpec.config);
  }


  @Override
  public String toString() {
    return "TopicSpec={" +
        "name='" + name() + '\'' +
        ", partitions='" + partitions() + '\'' +
        ", replicationFactor=" + replicationFactor() +
        ", config=" + config() +
        '}';
  }

  public static class Topics{
    public List<TopicSpec> topics;

    public Topics() {
    }

    public List<TopicSpec> topics() {
      return topics;
    }

    public void setTopics(List<TopicSpec> topics) {
      this.topics = topics;
    }

    @Override
    public String toString() {
      return "Topics{" +
          "topics=" + topics +
          '}';
    }
  }
}
