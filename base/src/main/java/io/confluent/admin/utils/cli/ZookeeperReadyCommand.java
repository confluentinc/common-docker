/**
 * Copyright 2016 Confluent Inc.
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

package io.confluent.admin.utils.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.confluent.admin.utils.ClusterStatus;

import static net.sourceforge.argparse4j.impl.Arguments.store;

/**
 * This command checks if the zookeeper cluster is ready to accept requests.
 * where:
 * <zookeeper_connect>  : Zookeeper connect string
 * <timeout>            : timeout in millisecs for all operations.
 */
public class ZookeeperReadyCommand {

  private static final Logger log = LoggerFactory.getLogger(ZookeeperReadyCommand.class);
  public static final String ZK_READY = "zk-ready";

  private static ArgumentParser createArgsParser() {
    ArgumentParser zkReady = ArgumentParsers
        .newArgumentParser(ZK_READY)
        .defaultHelp(true)
        .description("Check if ZK is ready.");

    zkReady.addArgument("zookeeper_connect")
        .action(store())
        .required(true)
        .type(String.class)
        .metavar("ZOOKEEPER_CONNECT")
        .help("Zookeeper connect string.");

    zkReady.addArgument("timeout")
        .action(store())
        .required(true)
        .type(Integer.class)
        .metavar("TIMEOUT_IN_MS")
        .help("Time (in ms) to wait for service to be ready.");

    return zkReady;
  }

  public static void main(String[] args) {

    ArgumentParser parser = createArgsParser();
    boolean success;
    try {
      Namespace res = parser.parseArgs(args);
      log.debug("Arguments {}. ", res);

      success = ClusterStatus.isZookeeperReady(
          res.getString("zookeeper_connect"),
          res.getInt("timeout")
      );

    } catch (ArgumentParserException e) {
      if (args.length == 0) {
        parser.printHelp();
        success = true;
      } else {
        parser.handleError(e);
        success = false;
      }
    } catch (Exception e) {
      log.error("Error while running zk-ready {}.", e);
      success = false;
    }

    if (success) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }
}
