package io.confluent.common.utils;

import org.apache.zookeeper.data.Stat;

public class ZkData {

  private final String data;
  private final Stat stat;

  public ZkData(String data, Stat stat) {
    this.data = data;
    this.stat = stat;
  }

  public String getData() {
    return this.data;
  }

  public Stat getStat() {
    return this.stat;
  }
}
