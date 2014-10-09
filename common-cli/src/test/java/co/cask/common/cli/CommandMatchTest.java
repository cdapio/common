/*
 * Copyright © 2012-2014 Cask Data, Inc.
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

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * Test for {@link CommandMatch}.
 */
public class CommandMatchTest {

  private static final String NAME = "\"test cluster\"";
  private static final String TEMPLATE = "\"hadoop\"";
  private static final String SETTINGS = "'{some settings}'";
  private static final String OPTIONAL_ARG = "arg 1";
  private static final int SIZE = 5;
  private static final Command TEST_COMMAND = new Command() {
    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {

    }

    @Override
    public String getPattern() {
      return "create cluster <name> with template <template> of size <size>" +
        "[ using settings <settings>][ with <optional arg>]";
    }

    @Override
    public String getDescription() {
      return "Creates the cluster";
    }
  };

  @Test
  public void getArgumentsAllOptionalTest() throws NoSuchFieldException, IllegalAccessException {
    String testInput = new StringBuilder("create cluster ")
      .append(NAME)
      .append(" with template ")
      .append(TEMPLATE)
      .append(" of size ")
      .append(SIZE)
      .append(" using settings ")
      .append(SETTINGS)
      .append(" with ")
      .append(OPTIONAL_ARG).toString();
    CommandMatch commandMatch = new CommandMatch(TEST_COMMAND, testInput);
    Arguments args = commandMatch.getArguments();
    Assert.assertEquals(5, args.size());
    Assert.assertEquals(NAME, args.get("name"));
    Assert.assertEquals(TEMPLATE, args.get("template"));
    Assert.assertEquals(SIZE, args.getInt("size"));
    Assert.assertEquals(SETTINGS, args.get("settings"));
    Assert.assertEquals(OPTIONAL_ARG, args.get("optional arg"));
  }

  @Test
  public void getArgumentsSomeOptionalTest() throws NoSuchFieldException, IllegalAccessException {
    String testInput = new StringBuilder("create cluster ")
      .append(NAME)
      .append(" with template ")
      .append(TEMPLATE)
      .append(" of size ")
      .append(SIZE)
      .append(" with ")
      .append(OPTIONAL_ARG).toString();
    CommandMatch commandMatch = new CommandMatch(TEST_COMMAND, testInput);
    Arguments args = commandMatch.getArguments();
    Assert.assertEquals(4, args.size());
    Assert.assertEquals(NAME, args.get("name"));
    Assert.assertEquals(TEMPLATE, args.get("template"));
    Assert.assertEquals(SIZE, args.getInt("size"));
    Assert.assertEquals(OPTIONAL_ARG, args.get("optional arg"));
  }

  @Test
  public void getArgumentsNoOptionalTest() throws NoSuchFieldException, IllegalAccessException {
    String testInput = new StringBuilder("create cluster ")
      .append(NAME)
      .append(" with template ")
      .append(TEMPLATE)
      .append(" of size ")
      .append(SIZE).toString();
    CommandMatch commandMatch = new CommandMatch(TEST_COMMAND, testInput);
    Arguments args = commandMatch.getArguments();
    Assert.assertEquals(3, args.size());
    Assert.assertEquals(NAME, args.get("name"));
    Assert.assertEquals(TEMPLATE, args.get("template"));
    Assert.assertEquals(SIZE, args.getInt("size"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getArgumentsWrongMandatoryInputTest() throws NoSuchFieldException, IllegalAccessException {
    String testInput = new StringBuilder("create cluster ")
      .append(NAME)
      .append(" with template ")
      .append(" of size ")
      .append(SIZE).toString();
    CommandMatch commandMatch = new CommandMatch(TEST_COMMAND, testInput);
    commandMatch.getArguments();
  }

  @Test(expected = IllegalArgumentException.class)
  public void getArgumentsWrongOptionalInputTest() throws NoSuchFieldException, IllegalAccessException {
    String testInput = new StringBuilder("create cluster ")
      .append(NAME)
      .append(" with template ")
      .append(TEMPLATE)
      .append(" of size ")
      .append(SIZE)
      .append(" using settings ")
      .append(" with ")
      .append(OPTIONAL_ARG).toString();
    CommandMatch commandMatch = new CommandMatch(TEST_COMMAND, testInput);
    commandMatch.getArguments();
  }
}
