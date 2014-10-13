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

import java.io.IOException;
import java.io.PrintStream;

/**
 * Demonstrates simple usage of {@link CLI}.
 */
public class HelloCLI {

  public static void main(String[] args) throws IOException {
    CLI cli = new CLI<Command>(new EchoCommand());
    cli.startInteractiveMode(System.out);
  }

  /**
   * Echoes whatever is passed in.
   */
  public static class EchoCommand implements Command {

    @Override
    public void execute(Arguments arguments, PrintStream output) throws Exception {
      output.println(arguments.get("some-input"));
    }

    @Override
    public String getPattern() {
      return "echo <some-input>";
    }

    @Override
    public String getDescription() {
      return "Echoes whatever is passed";
    }
  }

}
