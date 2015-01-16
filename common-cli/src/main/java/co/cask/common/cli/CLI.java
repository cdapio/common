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

import co.cask.common.cli.completers.DefaultStringsCompleter;
import co.cask.common.cli.exception.CLIExceptionHandler;
import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.common.cli.internal.TreeNode;
import co.cask.common.cli.supplier.CompleterSupplier;
import co.cask.common.cli.supplier.DefaultCompleterSupplier;
import co.cask.common.cli.util.Parser;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.AggregateCompleter;
import jline.console.completer.Completer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Provides a command-line interface (CLI) with auto-completion,
 * interactive and non-interactive modes, and other typical shell features.
 * </p>
 * <p/>
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

  private final ConsoleReader reader;
  private final CompleterSupplier defaultCompleterSupplier;

  private CommandSet<T> commands;
  private CompleterSet completers;
  private List<UserInterruptHandler> userInterruptHandlers;
  private List<CompleterSupplier> completerSuppliers;

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
    this.defaultCompleterSupplier = new DefaultCompleterSupplier();
    this.completerSuppliers = Lists.newArrayList();
    userInterruptHandlers = Lists.newArrayList();
  }

  /**
   * @param commands the commands to use
   * @throws IOException if unable to construct the {@link ConsoleReader}.
   */
  public CLI(T... commands) throws IOException {
    this(ImmutableList.copyOf(commands), ImmutableMap.<String, Completer>of());
  }

  /**
   * Set {@link CLI} {@link CommandSet}.
   *
   * @param commands the commands to update
   */
  public void setCommands(Iterable<T> commands) {
    this.commands = new CommandSet<T>(commands);
    updateCompleters();
  }

  /**
   * Set {@link CLI} {@link CompleterSet}.
   *
   * @param completers the completers to update
   */
  public void setCompleters(Map<String, Completer> completers) {
    this.completers = new CompleterSet(completers);
    updateCompleters();
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
    try {
      CommandMatch match = commands.findMatch(input);
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
    updateCompleters();

    while (true) {
      String line;

      try {
        line = reader.readLine();
      } catch (UserInterruptException e) {
        for (UserInterruptHandler handler : userInterruptHandlers) {
          handler.onUserInterrupt();
        }
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

  private void updateCompleters() {
    for (Completer completer : reader.getCompleters()) {
      reader.removeCompleter(completer);
    }
    List<Completer> completerList = generateCompleters();
    for (Completer completer : completerList) {
      reader.addCompleter(completer);
    }
  }

  public void setExceptionHandler(CLIExceptionHandler<Exception> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  private List<Completer> generateCompleters() {
    TreeNode<String> commandTokenTree = new TreeNode<String>();

    for (Command command : commands) {
      String pattern = command.getPattern();
      List<String> tokens = Parser.parsePattern(pattern);

      generateCompleters(commandTokenTree, tokens);
    }

    return generateCompleters(null, commandTokenTree);
  }

  private TreeNode<String> generateCompleters(TreeNode<String> commandTokenTree, List<String> tokens) {
    TreeNode<String> currentNode = commandTokenTree;
    int counter = 1;
    for (String token : tokens) {
      if (token.matches("\\[.+\\]")) {
        List<String> subTokens = Parser.parsePattern(getEntry(token));
        subTokens.addAll(tokens.subList(counter, tokens.size()));
        currentNode = generateCompleters(currentNode, subTokens);
      } else {
        currentNode = currentNode.findOrCreateChild(token);
      }
      counter++;
    }

    return commandTokenTree;
  }

  private List<Completer> generateCompleters(String prefix, TreeNode<String> commandTokenTree) {
    List<Completer> completers = Lists.newArrayList();
    String name = commandTokenTree.getData();
    String childPrefix = (prefix == null || prefix.isEmpty() ? "" : prefix + " ") + (name == null ? "" : name);

    if (!commandTokenTree.getChildren().isEmpty()) {
      List<String> nonArgumentTokens = Lists.newArrayList();
      List<String> argumentTokens = Lists.newArrayList();
      for (TreeNode<String> child : commandTokenTree.getChildren()) {
        String childToken = child.getData();
        if (childToken.matches("<.+>")) {
          argumentTokens.add(childToken);
        } else {
          nonArgumentTokens.add(child.getData());
        }
      }

      for (String argumentToken : argumentTokens) {
        // chop off the < and >
        String completerType = getEntry(argumentToken);
        Completer argumentCompleter = getCompleterForType(completerType);
        if (argumentCompleter != null) {
          completers.add(getCompleter(childPrefix, argumentCompleter));
        }
      }

      if (!nonArgumentTokens.isEmpty()) {
        completers.add(getCompleter(childPrefix, new DefaultStringsCompleter(nonArgumentTokens)));
      }

      for (TreeNode<String> child : commandTokenTree.getChildren()) {
        completers.addAll(generateCompleters(childPrefix, child));
      }
    }

    return Lists.<Completer>newArrayList(new AggregateCompleter(completers));
  }

  /**
   * Retrieves entry from input {@link String}.
   * For example, for input "<some input>" returns "some input".
   *
   * @param input the input
   * @return entry {@link String}
   */
  private String getEntry(String input) {
    Preconditions.checkArgument(input != null);
    return input.substring(1, input.length() - 1);
  }

  private Completer getCompleter(String prefix, Completer completer) {
    Completer customCompleter;
    for (CompleterSupplier supplier : completerSuppliers) {
      customCompleter = supplier.getCompleter(prefix, completer);
      if (customCompleter != null) {
        return customCompleter;
      }
    }
    return defaultCompleterSupplier.getCompleter(prefix, completer);
  }

  public void addCompleterSupplier(CompleterSupplier completerSupplier) {
    this.completerSuppliers.add(completerSupplier);
  }

  private Completer getCompleterForType(String completerType) {
    return completers.getCompleter(completerType);
  }

  public void addUserInterruptHandler(UserInterruptHandler handler) {
    this.userInterruptHandlers.add(handler);
  }

  /**
   * Handler to handle user interrupt.
   */
  public interface UserInterruptHandler {
    void onUserInterrupt() throws IOException;
  }
}
