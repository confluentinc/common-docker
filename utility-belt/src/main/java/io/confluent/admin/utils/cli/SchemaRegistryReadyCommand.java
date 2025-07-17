/*
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

package io.confluent.admin.utils.cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static net.sourceforge.argparse4j.impl.Arguments.store;

/**
 * This command checks if the Schema Registry is ready to accept requests.
 * where:
 * host     : Hostname for Schema Registry.
 * port     : Port for Schema Registry.
 * timeout  : Time in secs to wait for service to be ready.
 */
public class SchemaRegistryReadyCommand {

  private static final Logger log = LogManager.getLogger(SchemaRegistryReadyCommand.class);
  public static final String SR_READY = "sr-ready";
  private static final String HEALTH_ENDPOINT = "/subjects";

  private static ArgumentParser createArgsParser() {
    ArgumentParser srReady = ArgumentParsers
        .newArgumentParser(SR_READY)
        .defaultHelp(true)
        .description("Check if Schema Registry is ready.");

    srReady.addArgument("host")
        .action(store())
        .required(true)
        .type(String.class)
        .metavar("HOST")
        .help("Hostname for Schema Registry.");

    srReady.addArgument("port")
        .action(store())
        .required(true)
        .type(Integer.class)
        .metavar("PORT")
        .help("Port for Schema Registry.");

    srReady.addArgument("timeout")
        .action(store())
        .required(true)
        .type(Integer.class)
        .metavar("TIMEOUT_IN_SECS")
        .help("Time (in secs) to wait for service to be ready.");

    return srReady;
  }

  public static void main(String[] args) {
    ArgumentParser parser = createArgsParser();
    boolean success = false;
    try {
      Namespace res = parser.parseArgs(args);
      log.debug("Arguments {}. ", res);

      String host = res.getString("host");
      int port = res.getInt("port");
      int timeoutSecs = res.getInt("timeout");

      success = isSchemaRegistryReady(host, port, timeoutSecs);

    } catch (ArgumentParserException e) {
      if (args.length == 0) {
        parser.printHelp();
        success = true;
      } else {
        parser.handleError(e);
        success = false;
      }
    } catch (Exception e) {
      log.error("Error while running sr-ready.", e);
      success = false;
    }

    if (success) {
      System.exit(0);
    } else {
      System.exit(1);
    }
  }

  /**
   * Checks if Schema Registry is ready by:
   * 1. First checking if the port is open
   * 2. Then making an HTTP request to the /subjects endpoint
   *
   * @param host Hostname for Schema Registry
   * @param port Port for Schema Registry
   * @param timeoutSecs Timeout in seconds
   * @return true if Schema Registry is ready, false otherwise
   */
  public static boolean isSchemaRegistryReady(String host, int port, int timeoutSecs) {
    log.debug("Check if Schema Registry is ready: {}:{}", host, port);
    
    long startTime = System.currentTimeMillis();
    long timeoutMs = TimeUnit.SECONDS.toMillis(timeoutSecs);
    
    while (System.currentTimeMillis() - startTime < timeoutMs) {
      try {
        // First check if the port is open
        if (!isPortOpen(host, port, 5000)) { // 5 second connection timeout
          log.debug("Port {} is not open on host {}", port, host);
          Thread.sleep(1000);
          continue;
        }
        
        // Then check if Schema Registry responds to HTTP requests
        if (isHttpEndpointReady(host, port, 5000)) { // 5 second HTTP timeout
          log.info("Schema Registry is ready at {}:{}", host, port);
          return true;
        }
        
        log.debug("Schema Registry HTTP endpoint not ready at {}:{}", host, port);
        Thread.sleep(1000);
        
      } catch (Exception e) {
        log.debug("Error checking Schema Registry readiness: {}", e.getMessage());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return false;
        }
      }
    }
    
    log.error("Schema Registry is not ready at {}:{} after {} seconds", host, port, timeoutSecs);
    return false;
  }

  /**
   * Checks if a port is open on the given host
   */
  private static boolean isPortOpen(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Checks if the Schema Registry HTTP endpoint is ready by making a request to /subjects
   */
  private static boolean isHttpEndpointReady(String host, int port, int timeoutMs) {
    try {
      String urlString = String.format("http://%s:%d%s", host, port, HEALTH_ENDPOINT);
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(timeoutMs);
      connection.setReadTimeout(timeoutMs);
      
      int responseCode = connection.getResponseCode();
      connection.disconnect();
      
      // Schema Registry returns 200 for /subjects endpoint when ready
      return responseCode == 200;
      
    } catch (IOException e) {
      log.debug("HTTP request failed: {}", e.getMessage());
      return false;
    }
  }
} 