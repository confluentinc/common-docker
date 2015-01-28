package io.confluent.common.utils.zookeeper;

import org.I0Itec.zkclient.ZkClient;

public interface ConditionalUpdateCallback {

  public int checker(ZkClient client, String path, String data);
  
}
