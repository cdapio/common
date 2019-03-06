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

package io.cdap.common.cli;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.cdap.common.cli.exception.InvalidCommandException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * Test for {@link CommandSet}.
 */
public class CommandSetTest {

  @Test
  public void testFindMatch() throws Exception {
    Command greetCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        output.println("truncated!");
      }

      @Override
      public String getPattern() {
        return "truncate all streams";
      }

      @Override
      public String getDescription() {
        return "Truncates all streams";
      }
    };

    final CommandSet commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    CommandMatch match = commandSet.findMatch("truncate all streams");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "truncated!\n");

    Function<Exception, Void> invalidCommandValidator = new Function<Exception, Void>() {
      @Override
      public Void apply(Exception input) {
        Assert.assertTrue(input instanceof InvalidCommandException);
        return null;
      }
    };

    assertThrows(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        commandSet.findMatch("truncate all streams!");
        return null;
      }
    }, invalidCommandValidator);

    assertThrows(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        commandSet.findMatch("truncate no streams");
        return null;
      }
    }, invalidCommandValidator);

    assertThrows(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        commandSet.findMatch("truncate all streams x");
        return null;
      }
    }, invalidCommandValidator);

    assertThrows(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        commandSet.findMatch("x truncate all streams");
        return null;
      }
    }, invalidCommandValidator);
  }

  private void assertThrows(Callable<Void> callable, Function<Exception, Void> exceptionValidator) {
    try {
      callable.call();
      Assert.fail();
    } catch (Exception e) {
      exceptionValidator.apply(e);
    }
  }

  @Test
  public void testFindMatchWithSpaces() throws Exception {
    Command greetCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        output.println("Hello x!");
      }

      @Override
      public String getPattern() {
        return "greet user x";
      }

      @Override
      public String getDescription() {
        return "Greets x";
      }
    };

    CommandSet commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    CommandMatch match = commandSet.findMatch("greet      user x");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "Hello x!\n");

    commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    match = commandSet.findMatch("greet user       x");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "Hello x!\n");

    commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    match = commandSet.findMatch("      greet user x");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "Hello x!\n");

    commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    match = commandSet.findMatch("greet user x        ");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "Hello x!\n");

    commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    match = commandSet.findMatch("        greet      user       x     ");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), "Hello x!\n");
  }

  @Test
  public void testFindMatchWithArguments() throws Exception {
    Command greetCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        for (int i = 0; i < arguments.getInt("times", 1); i++) {
          output.println("Hello " + arguments.get("user"));
        }
      }

      @Override
      public String getPattern() {
        return "greet <user> times <times>";
      }

      @Override
      public String getDescription() {
        return "Greets a user";
      }
    };

    CommandSet commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    CommandMatch match = commandSet.findMatch("greet bob times 5");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), Strings.repeat("Hello bob\n", 5));
  }

  @Test
  public void testFindMatchWithOptionalArguments() throws Exception {
    Command greetCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        for (int i = 0; i < arguments.getInt("times", 1); i++) {
          output.printf("[%d] Hello %s %s\n", arguments.getInt("timestamp", 111),
                        arguments.get("user"), arguments.get("suffix", "oneoneone"));
        }
      }

      @Override
      public String getPattern() {
        return "greet <user> times <times> [timestamp <timestamp>] [suffix <suffix>]";
      }

      @Override
      public String getDescription() {
        return "Greets a user";
      }
    };

    CommandSet commandSet = new CommandSet<Command>(ImmutableList.of(greetCommand));
    CommandMatch match = commandSet.findMatch("greet bob times 5 timestamp 123 suffix blah");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), Strings.repeat("[123] Hello bob blah\n", 5));

    match = commandSet.findMatch("greet bob times 5");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), Strings.repeat("[111] Hello bob oneoneone\n", 5));

    match = commandSet.findMatch("greet bob times 5 suffix blah");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), Strings.repeat("[111] Hello bob blah\n", 5));

    match = commandSet.findMatch("greet bob times 5 timestamp 321");
    Assert.assertTrue(match.getCommand() == greetCommand);
    testCommand(match.getCommand(), match.getArguments(), Strings.repeat("[321] Hello bob oneoneone\n", 5));
  }

  @Test
  public void testFindMatchSimilarPrefix() throws Exception {
    Command getClusterCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
          output.println("get cluster command with id: " + arguments.get("id"));
      }

      @Override
      public String getPattern() {
        return "get cluster <id>";
      }

      @Override
      public String getDescription() {
        return "Gets the cluster";
      }
    };

    Command getClusterConfigCommand = new Command() {
      @Override
      public void execute(Arguments arguments, PrintStream output) throws Exception {
        output.println("get cluster config command with id: " + arguments.get("id"));
      }

      @Override
      public String getPattern() {
        return "get cluster-config <id>";
      }

      @Override
      public String getDescription() {
        return "Gets the cluster config";
      }
    };

    CommandSet commandSet = new CommandSet<Command>(ImmutableList.of(getClusterCommand, getClusterConfigCommand));
    CommandMatch match1 = commandSet.findMatch("get cluster test1");
    Assert.assertTrue(match1.getCommand() == getClusterCommand);
    testCommand(match1.getCommand(), match1.getArguments(), "get cluster command with id: test1\n");

    CommandMatch match2 = commandSet.findMatch("get cluster-config test2");
    Assert.assertTrue(match2.getCommand() == getClusterConfigCommand);
    testCommand(match2.getCommand(), match2.getArguments(), "get cluster config command with id: test2\n");
  }

  private void testCommand(Command command, Arguments args, String expectedOutput) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(outputStream);
    command.execute(args, printStream);

    String output = new String(outputStream.toByteArray(), Charsets.UTF_8);
    Assert.assertEquals(expectedOutput, output);
  }
}
