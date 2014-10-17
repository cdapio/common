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

package co.cask.common.cli.completers;

import co.cask.common.cli.Command;
import co.cask.common.cli.CommandSet;
import co.cask.common.cli.CompleterSet;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jline.console.completer.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Completer for a set of strings.
 *
 * @param <T> type of command
 */
public class CLICompleter<T extends Command> extends AbstractCompleter {

  private static final Logger LOG = LoggerFactory.getLogger(CLICompleter.class);

  private final CommandSet<T> commands;
  private final CompleterSet completers;

  public CLICompleter(CommandSet<T> commands, CompleterSet completers) {
    this.commands = commands;
    this.completers = completers;
  }

  @Override
  protected List<CharSequence> getAllCandidates() {
    List<CharSequence> allCandidates = Lists.newArrayList();
    for (T command : commands.getCommands()) {
      String firstToken = command.getPattern().split(" ")[0];
      if (!firstToken.matches("<.+?>") && !firstToken.matches("\\[.+?\\]")) {
        allCandidates.add(firstToken);
      }
    }
    return allCandidates;
  }

  @Override
  protected List<CharSequence> getCandidates(String buffer) {
    LOG.info("Getting candidates for {}", Arrays.toString(buffer.toCharArray()));
    List<CharSequence> candidates = Lists.newArrayList();

    for (T command : commands.getCommands()) {
      if (command.getPattern().startsWith(buffer)) {
        String[] bufferTokens = buffer.split(" ");
        String[] patternTokens = command.getPattern().split(" ");
        String lastBufferToken = bufferTokens[bufferTokens.length - 1];
        String lastPatternToken = patternTokens[bufferTokens.length - 1];
        if (lastPatternToken.equals(lastBufferToken)) {
          lastPatternToken = patternTokens[bufferTokens.length];
        }

        if (!lastPatternToken.matches("<.+?>") && !lastPatternToken.matches("\\[.+?\\]")) {
          // got a non-argument token
          candidates.add(lastPatternToken);
          LOG.info("Added non-argument token: {}", lastPatternToken);
        } else {
          // got an argument token (e.g. <someArg> or [someArg]) - use completer
          String argumentName = lastPatternToken.substring(1, lastPatternToken.length() - 1);
          Completer completer = completers.getCompleter(argumentName);
          if (completer != null) {
            LOG.info("Invoking argument completer: " + argumentName);
            List<CharSequence> argumentCandidates = Lists.newArrayList();
            completer.complete(lastPatternToken, 0, argumentCandidates);
            candidates.addAll(argumentCandidates);
          }
        }
      }
    }

    return candidates;
  }

}
