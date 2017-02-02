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
 **/
package io.confluent.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigUtils {
  private final static Logger log = LoggerFactory.getLogger(ConfigUtils.class);

  public static Properties translateDeprecated(Properties props, String[][] keyArray) {
    Properties newProps = new Properties(props);
    for (String[] keyInfo: keyArray) {
      String target = keyInfo[0];
      List<String> deprecated = new ArrayList<>();
      for (int i = 1; i < keyInfo.length; i++) {
        if (props.containsKey(keyInfo[i])) {
          deprecated.add(keyInfo[i]);
        }
      }
      if (deprecated.isEmpty()) {
        // No deprecated key(s) found.
        continue;
      }
      String synonymString = deprecated.get(0);
      for (int i = 1; i < deprecated.size(); i++) {
        synonymString += ", " + deprecated.get(i);
      }
      if (props.containsKey(target)) {
        log.error(target + " was configured, as well as the deprecated synonym(s) " +
          synonymString + ".  Using the value of " + target);
        // Ignore the deprecated key(s) because the actual key was set.
      } else if (deprecated.size() > 1) {
        log.error("The configuration keys " + synonymString + " are deprecated and may be " +
          "removed in the future.  Additionally, this configuration is ambigous because " +
          "these configuration keys are all synonyms for " + target + ".  Please update " +
          "your configuration to have only " + target + " set.");
        newProps.setProperty(target, props.getProperty(deprecated.get(0)));
      } else {
        log.warn("Configuration key " + deprecated.get(0) + " is deprecated and may be removed " +
          "in the future.  Please update your configuration to use " + target + " instead.");
        newProps.setProperty(target, props.getProperty(deprecated.get(0)));
      }
    }
    return newProps;
  }
}
