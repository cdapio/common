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
package co.cask.common.cli.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test for {@link Parser}.
 */
public class ParserTest {

  @Test
  public void testConsecutiveSeparators() {
    List<String> strings = Parser.parseInput("      create cluster  '%s z' with  template     '%s' of  size '%s'  ");
    Assert.assertEquals("create", strings.get(0));
    Assert.assertEquals("cluster", strings.get(1));
    Assert.assertEquals("'%s z'", strings.get(2));
    Assert.assertEquals("with", strings.get(3));
    Assert.assertEquals("template", strings.get(4));
    Assert.assertEquals("'%s'", strings.get(5));
    Assert.assertEquals("of", strings.get(6));
    Assert.assertEquals("size", strings.get(7));
    Assert.assertEquals("'%s'", strings.get(8));
    Assert.assertEquals(9, strings.size());
  }

}
