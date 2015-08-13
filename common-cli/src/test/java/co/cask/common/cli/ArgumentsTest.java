/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.common.cli;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link Arguments}.
 */
public class ArgumentsTest {

  @Test
  public void testGetString() {
    Arguments arguments = new Arguments(ImmutableMap.of("a", "b"), "");
    Assert.assertEquals("b", arguments.get("a"));
    Assert.assertEquals("z", arguments.getOptional("123", "z"));
    Assert.assertEquals(null, arguments.getOptional("123", null));
    Assert.assertEquals(null, arguments.getOptional("123"));
  }

  @Test
  public void testGetLong() {
    Arguments arguments = new Arguments(ImmutableMap.of("a", Long.toString(Long.MAX_VALUE)), "");
    Assert.assertEquals(Long.MAX_VALUE, (long) arguments.getLong("a"));
    Assert.assertEquals(24L, (long) arguments.getLongOptional("123", 24L));
    Assert.assertEquals(null, arguments.getLongOptional("123", null));
    Assert.assertEquals(null, arguments.getLongOptional("123"));
  }

  @Test
  public void testGetInt() {
    Arguments arguments = new Arguments(ImmutableMap.of("a", Integer.toString(Integer.MAX_VALUE)), "");
    Assert.assertEquals(Integer.MAX_VALUE, (long) arguments.getInt("a"));
    Assert.assertEquals(24, (long) arguments.getIntOptional("123", 24));
    Assert.assertEquals(null, arguments.getIntOptional("123"));
  }
}
