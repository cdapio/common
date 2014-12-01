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
import co.cask.common.cli.exception.InvalidCommandException;
import com.google.common.base.Supplier;

import java.io.PrintStream;
import java.util.List;

/**
 * Prints usage information and description of commands.
 */
public class HelpCommand implements Command {

  private static final String COMMAND_KEY = "command";

  private final Supplier<CommandSet<Command>> getCommands;
  private final String helpHeader;

  public HelpCommand(Supplier<CommandSet<Command>> getCommands) {
    this(getCommands, null);
  }

  public HelpCommand(Supplier<CommandSet<Command>> getCommands, String helpHeader) {
    this.getCommands = getCommands;
    this.helpHeader = helpHeader;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    if (arguments.hasArgument(COMMAND_KEY)) {
      String input = arguments.get(COMMAND_KEY);
      List<Command> commands = getCommands.get().findMatchCommands(input);
      if (commands.isEmpty()) {
        printStream.println(String.format("No appropriate commands for pattern: %s", input));
      } else {
        printStream.println("Available commands:");
        for (Command command : commands) {
          printStream.println(String.format("%s: %s", command.getPattern(), command.getDescription()));
        }
      }
    } else {
      if (helpHeader != null) {
        printStream.println(helpHeader);
        printStream.println();
      }
      printStream.println(String.format("Available commands: \n%s: %s", getPattern(), getDescription()));
      for (Command command : getCommands.get()) {
        printStream.println(String.format("%s: %s", command.getPattern(), command.getDescription()));
      }
    }
    printStream.println();
  }

  @Override
  public String getPattern() {
    return String.format("help [<%s>]", COMMAND_KEY);
  }

  @Override
  public String getDescription() {
    return "Prints usage information and description of commands";
  }
}
