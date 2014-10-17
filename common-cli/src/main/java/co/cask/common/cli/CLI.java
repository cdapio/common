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

import co.cask.common.cli.completers.CLICompleter;
import co.cask.common.cli.exception.CLIExceptionHandler;
import co.cask.common.cli.exception.InvalidCommandException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * <p>
 * Provides a command-line interface (CLI) with auto-completion,
 * interactive and non-interactive modes, and other typical shell features.
 * </p>
 *
 * <p>
 * {@link #commands} contains all of the available commands, and {@link #completers}
 * contains the available completers per argument type. For example, if we have a command
 * with the pattern "start flow <flow-id>" and a completer keyed by "flow-id" in the {@link #completers} map,
 * then when the user enters "start flow" and then hits TAB, the completer will be activated to provide
 * auto-completion.
 * </p>
 *
 * @param <T> type of {@link Command} that this {@link CLI} will use
 */
public class CLI<T extends Command> {

  private final CommandSet<T> commands;
  private final CompleterSet completers;
  private final ConsoleReader reader;

  private CLIExceptionHandler<Exception> exceptionHandler = new CLIExceptionHandler<Exception>() {
    @Override
    public void handleException(PrintStream output, Exception exception) {
      output.println("Error: " + exception.getMessage());
    }
  };

  /**
   * @param commands the commands to use
   * @param completers the completers to use
   * @throws IOException if unable to construct the {@link ConsoleReader}.
   */
  public CLI(Iterable<T> commands, Map<String, Completer> completers) throws IOException {
    this.commands = new CommandSet<T>(commands);
    this.completers = new CompleterSet(completers);
    this.reader = new ConsoleReader();
    this.reader.setPrompt("cli> ");
  }

  /**
   * @param commands the commands to use
   * @throws IOException if unable to construct the {@link ConsoleReader}.
   */
  public CLI(T... commands) throws IOException {
    this(ImmutableList.copyOf(commands), ImmutableMap.<String, Completer>of());
  }

  /**
   * @return the {@link ConsoleReader} that is being used to read input.
   */
  public ConsoleReader getReader() {
    return reader;
  }

  /**
   * Executes a command given some input.
   *
   * @param input the input
   * @param output the {@link PrintStream} to write messages to
   */
  public void execute(String input, PrintStream output) throws InvalidCommandException {
    CommandMatch match = commands.findMatch(input);
    try {
      match.getCommand().execute(match.getArguments(), output);
    } catch (Exception e) {
      exceptionHandler.handleException(output, e);
    }
  }

  /**
   * Starts interactive mode, which provides a shell to enter multiple commands and use auto-completion.
   *
   * @param output {@link java.io.PrintStream} to write to
   * @throws java.io.IOException if there's an issue in reading the input
   */
  public void startInteractiveMode(PrintStream output) throws IOException {
    this.reader.setHandleUserInterrupt(true);
    this.reader.addCompleter(new CLICompleter<T>(commands, completers));

    while (true) {
      String line;

      try {
        line = reader.readLine();
      } catch (UserInterruptException e) {
        continue;
      }

      if (line == null) {
        output.println();
        break;
      }

      if (line.length() > 0) {
        String command = line.trim();
        try {
          execute(command, output);
        } catch (Exception e) {
          exceptionHandler.handleException(output, e);
        }
        output.println();
      }
    }
  }

  public void setExceptionHandler(CLIExceptionHandler<Exception> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  private Completer getCompleterForType(String completerType) {
    return completers.getCompleter(completerType);
  }

}
