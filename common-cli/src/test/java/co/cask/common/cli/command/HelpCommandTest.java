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


package co.cask.common.cli.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.common.cli.CommandMatch;
import co.cask.common.cli.CommandSet;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Tests for {@link HelpCommand}
 */
public class HelpCommandTest {

  private static final String HELP_HEADER = "Help header";
  private static final String TEST_COMMAND1_PATTERN = "get something <param>";
  private static final String TEST_COMMAND1_DESCRIPTION = "Test command 1 description";
  private static final String TEST_COMMAND2_PATTERN = "get another thing";
  private static final String TEST_COMMAND2_DESCRIPTION = "Test command 2 description";
  private static final String TEST_COMMAND3_PATTERN = "list of something <param> [<optional>]";
  private static final String TEST_COMMAND3_DESCRIPTION = "Test command 3 description";
  private static final Command TEST_COMMAND1 = new Command() {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {

    }

    @Override
    public String getPattern() {
      return TEST_COMMAND1_PATTERN;
    }

    @Override
    public String getDescription() {
      return TEST_COMMAND1_DESCRIPTION;
    }
  };
  private static final Command TEST_COMMAND2 = new Command() {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {

    }

    @Override
    public String getPattern() {
      return TEST_COMMAND2_PATTERN;
    }

    @Override
    public String getDescription() {
      return TEST_COMMAND2_DESCRIPTION;
    }
  };
  private static final Command TEST_COMMAND3 = new Command() {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {

    }

    @Override
    public String getPattern() {
      return TEST_COMMAND3_PATTERN;
    }

    @Override
    public String getDescription() {
      return TEST_COMMAND3_DESCRIPTION;
    }
  };

  private static final CommandSet<Command> TEST_COMMAND_SET =
    new CommandSet<Command>(ImmutableList.of(TEST_COMMAND1, TEST_COMMAND2, TEST_COMMAND3));
  private static final HelpCommand HELP_COMMAND = new HelpCommand(TEST_COMMAND_SET, HELP_HEADER);
  private static final CommandSet<Command> TEST_SET =
    new CommandSet<Command>(ImmutableList.<Command>of(HELP_COMMAND), ImmutableList.of(TEST_COMMAND_SET));

  @Test
  public void helpCommandTest() throws Exception {
    CommandMatch match = TEST_SET.findMatch("help");
    testCommandOutput(match.getCommand(), match.getArguments(), createExpectedOutput(HELP_HEADER,
                                                                                     HELP_COMMAND, TEST_COMMAND1,
                                                                                     TEST_COMMAND2, TEST_COMMAND3));
  }

  @Test
  public void helpHelpCommandTest() throws Exception {
    CommandMatch match = TEST_SET.findMatch("help help");
    testCommandOutput(match.getCommand(), match.getArguments(), createExpectedOutput(null, HELP_COMMAND));
  }

  @Test
  public void helpWithOptionalParamCommandTest() throws Exception {
    CommandMatch match = TEST_SET.findMatch("help get");
    testCommandOutput(match.getCommand(), match.getArguments(), createExpectedOutput(null,
                                                                                     TEST_COMMAND1, TEST_COMMAND2));
  }

  @Test
  public void helpWithOptionalParamNoCommandsCommandTest() throws Exception {
    CommandMatch match = TEST_SET.findMatch("help set");
    testCommandOutput(match.getCommand(), match.getArguments(),
                      String.format("No appropriate commands for pattern: %s%s%s", "set",
                                    System.getProperty("line.separator"), System.getProperty("line.separator")));
  }

  private String createExpectedOutput(String header, Command... commands) {
    StringBuilder builder = new StringBuilder();
    if (header != null) {
      builder.append(header).append(System.getProperty("line.separator"));
      builder.append(System.getProperty("line.separator"));
    }
    builder.append("Available commands:").append(System.getProperty("line.separator"));
    for (Command command : commands) {
      builder.append(String.format("%s: %s", command.getPattern(), command.getDescription()))
        .append(System.getProperty("line.separator"));
    }
    builder.append(System.getProperty("line.separator"));
    return builder.toString();
  }

  private void testCommandOutput(Command command, Arguments args, String expectedOutput) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(outputStream);
    command.execute(args, printStream);

    String output = new String(outputStream.toByteArray(), Charsets.UTF_8);
    Assert.assertEquals(expectedOutput, output);
  }
}
