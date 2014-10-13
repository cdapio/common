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

package co.cask.common.cli.examples;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.CLI;
import co.cask.common.cli.Command;
import co.cask.common.cli.completers.StringsCompleter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import jline.console.completer.Completer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Example {@link CLI}.
 */
public class NoteCLI {

  private Map<String, String> notes = Maps.newHashMap();

  public NoteCLI() throws IOException {
    List<Command> commands = ImmutableList.of(
      new GetCommand(),
      new CreateCommand(),
      new DeleteCommand(),
      new ListCommand());

    Map<String, Completer> completers = ImmutableMap.<String, Completer>of(
      "note-id", new StringsCompleter() {
        @Override
        protected Supplier<Collection<String>> getStringsSupplier() {
          return Suppliers.<Collection<String>>ofInstance(notes.keySet());
        }
      }
    );

    CLI cli = new CLI<Command>(commands, completers);
    cli.startInteractiveMode(System.out);
  }

  /**
   * Creates a note.
   */
  public class CreateCommand implements Command {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {
      String noteId = arguments.get("new-note-id");
      String prefix = "create " + noteId + " ";
      String content = arguments.getRawInput().substring(prefix.length());
      notes.put(noteId, content);
      output.printf("Created note '%s'\n", noteId);
    }

    @Override
    public String getPattern() {
      return "create <new-note-id> <content>";
    }

    @Override
    public String getDescription() {
      return "Creates a note";
    }
  }

  /**
   * Deletes a note.
   */
  public class DeleteCommand implements Command {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {
      String noteId = arguments.get("note-id");
      notes.remove(noteId);
      output.printf("Deleted note '%s'\n", noteId);
    }

    @Override
    public String getPattern() {
      return "delete <note-id>";
    }

    @Override
    public String getDescription() {
      return "Deletes a note";
    }
  }

  /**
   * Lists all notes.
   */
  public class ListCommand implements Command {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {
      if (notes.isEmpty()) {
        output.println("No notes");
      } else {
        output.println("Notes:");
        for (String noteId : notes.keySet()) {
          output.println(noteId);
        }
      }
    }

    @Override
    public String getPattern() {
      return "list";
    }

    @Override
    public String getDescription() {
      return "Lists all notes";
    }
  }

  /**
   * Gets the contents of a note.
   */
  public class GetCommand implements Command {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {
      String noteId = arguments.get("note-id");
      if (notes.containsKey(noteId)) {
        String contents = notes.get(noteId);
        output.printf("Note '%s': %s\n", noteId, contents);
      } else {
        output.printf("Note '%s' doesn't exist\n", noteId);
      }
    }

    @Override
    public String getPattern() {
      return "get <note-id>";
    }

    @Override
    public String getDescription() {
      return "Gets a note";
    }
  }

  public static void main(String[] args) throws IOException {
    new NoteCLI();
  }

}
