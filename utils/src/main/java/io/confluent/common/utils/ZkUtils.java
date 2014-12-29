package io.confluent.common.utils;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.data.Stat;

public class ZkUtils {

  /**
   * Make sure a persistent path exists in ZK. Create the path if it does not exist.
   */
  public static void makeSurePersistentPathExists(ZkClient client, String path) {
    if (!client.exists(path)) {
      client.createPersistent(path, true); // won't throw NoNodeException or NodeExistsException
    }
  }

  /**
   * Create the parent path
   */
  private static void createParentPath(ZkClient client, String path) {
    String parentDir = path.substring(0, path.lastIndexOf('/'));
    if (parentDir.length() != 0) {
      client.createPersistent(parentDir, true);
    }
  }

  /**
   * Create an persistent node with the given path and data. Create parent nodes if necessary.
   */
  public static void createPersistentPath(ZkClient client, String path, String data) {
    try {
      client.createPersistent(path, data);
    } catch (ZkNoNodeException nne) {
      createParentPath(client, path);
      client.createPersistent(path, data);
    }
  }

  /**
   * Update the value of a persistent node with the given path and data. create parent directory if
   * necessary. Never throw NodeExistException. Return the updated path zkVersion
   */
  public static void updatePersistentPath(ZkClient client, String path, String data) {
    try {
      client.writeData(path, data);
    } catch (ZkNoNodeException nne) {
      createParentPath(client, path);
      try {
        client.createPersistent(path, data);
      } catch (ZkNodeExistsException nee) {
        client.writeData(path, data);
      }
    }
  }

  public static ZkData readData(ZkClient client, String path) {
    Stat stat = new Stat();
    String data = client.readData(path, stat);
    return new ZkData(data, stat);
  }

  public static ZkData readDataMaybeNull(ZkClient client, String path) {
    Stat stat = new Stat();
    String data = client.readData(path, stat);
    return new ZkData(data, stat);
  }
}
