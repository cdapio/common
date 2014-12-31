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

package co.cask.common.cli.util;

import co.cask.common.cli.Command;
import co.cask.common.cli.command.HelpCommand;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility interface for printing the {@link HelpCommand} output.
 */
public class DefaultHelpFormatter implements HelpFormatter {

  public void print(Iterable<Command> commands, PrintStream printStream) {
    List<Command> sortedCommands = new LinkedList<Command>();

    for (Command command : commands) {
      sortedCommands.add(command);
    }

    Collections.sort(sortedCommands, new Comparator<Command>() {
      @Override
      public int compare(Command o1, Command o2) {
        return o1.getPattern().compareTo(o2.getPattern());
      }
    });

    printStream.println("Available commands:");

    String previousCommandName = "";
    for (Command command : sortedCommands) {
      String[] commandName = command.getPattern().split(" ", 2);
      if (previousCommandName.compareTo(commandName[0]) != 0) {
        previousCommandName = commandName[0];
        printStream.println(String.format("%s", previousCommandName));
      }

      printStream.println(String.format("  • %s: %s", command.getPattern(), command.getDescription()));
    }
  }
}
