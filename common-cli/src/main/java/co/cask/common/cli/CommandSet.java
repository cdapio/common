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

import co.cask.common.cli.exception.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

/**
 * Set of {@link Command}s.
 *
 * @param <T> type of {@link Command} that this {@link CommandSet} will contain.
 */
public class CommandSet<T extends Command> implements Iterable<T> {

  private final List<T> commands;
  private final List<CommandSet<T>> commandSets;

  /**
   * @param commands commands to include
   * @param commandSets command sets to include
   */
  public CommandSet(Iterable<T> commands, Iterable<CommandSet<T>> commandSets) {
    this.commands = ImmutableList.copyOf(commands);
    this.commandSets = ImmutableList.copyOf(commandSets);
  }

  /**
   * @param commands commands to include
   */
  public CommandSet(Iterable<T> commands) {
    this(commands, ImmutableList.<CommandSet<T>>of());
  }

  /**
   * @return {@link Iterator} over the {@link #commands} and the {@link Command}s within the {@link #commandSets}
   */
  public Iterator<T> iterator() {
    return Iterables.concat(commands, Iterables.concat(commandSets)).iterator();
  }

  /**
   * Finds a matching command for the provided input.
   *
   * @param input the input string
   * @return the matching command and the parsed arguments
   */
  public CommandMatch findMatch(String input) throws InvalidCommandException {
    for (Command command : this) {
      String pattern = getMatchPattern(command.getPattern());
      if (input.matches(pattern)) {
        return new CommandMatch(command, input);
      }
    }

    throw new InvalidCommandException(input);
  }

  /**
   * Finds matching commands for the provided input.
   *
   * @param input the input string
   * @return the matching commands
   */
  public List<T> findMatchCommands(String input) {
    List<T> appropriateCommands = Lists.newArrayList();
    for (T command : this) {
      if (command.getPattern().startsWith(input)) {
        appropriateCommands.add(command);
      }
    }
    return appropriateCommands;
  }

  public Iterable<T> getCommands() {
    return Iterables.concat(commands, Iterables.concat(commandSets));
  }

  /**
   * Convert command pattern to regular expression that matches any input of this command
   *
   * @param pattern the command pattern
   * @return regular expression
   */
  private String getMatchPattern(String pattern) {
    String mandatoryPart = pattern.replaceAll("(\\s+?)\\[.+?\\]", "($1.+?(\\\\s|\\$))*");
    return mandatoryPart.replaceAll("<.+?>", ".+?");
  }
}
