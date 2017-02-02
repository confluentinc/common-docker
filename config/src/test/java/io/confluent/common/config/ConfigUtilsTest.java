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

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ConfigUtilsTest {
  @Test
  public void testTranslateDeprecated() throws Exception {
    Properties props = new Properties();
    props.setProperty("foo.bar", "baz");
    props.setProperty("foo.bar.deprecated", "quux");
    props.setProperty("chicken", "1");
    props.setProperty("rooster", "2");
    props.setProperty("hen", "3");
    props.setProperty("heifer", "moo");
    props.setProperty("blah", "blah");
    Properties newProps = ConfigUtils.translateDeprecated(props, new String[][]{
        { "foo.bar", "foo.bar.deprecated" },
        { "chicken", "rooster", "hen" },
        { "cow", "beef", "heifer", "steer" }
    });
    assertEquals("baz", newProps.getProperty("foo.bar"));
    assertEquals("1", newProps.getProperty("chicken"));
    assertEquals("moo", newProps.getProperty("cow"));
    assertEquals(null, props.getProperty("cow"));
    assertEquals("blah", newProps.getProperty("blah"));
  }
}
