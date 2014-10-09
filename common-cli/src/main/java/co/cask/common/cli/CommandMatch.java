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

import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Represents an input matching for a command and provided arguments.
 */
public final class CommandMatch {

  private final Command command;
  private final String input;

  /**
   * @param command the {@link Command} that was matched to the {@link #input}
   * @param input the input that was provided
   */
  public CommandMatch(Command command, String input) {
    this.command = command;
    this.input = input;
  }

  /**
   * @return the {@link Command} that was matched to the {@link #input}
   */
  public Command getCommand() {
    return command;
  }

  /**
   * @return the {@link Arguments} parsed from the {@link #input} and {@link #command} pattern
   */
  public Arguments getArguments() {
    return parseArguments(input, command.getPattern());
  }

  public String getInput() {
    return input;
  }

  private Arguments parseArguments(String input, String pattern) {
    ImmutableMap.Builder<String, String> args = ImmutableMap.builder();
    int mandatoryEnd = pattern.indexOf("[");
    mandatoryEnd = mandatoryEnd > 0 ? mandatoryEnd : pattern.length();
    String mandatoryPatternPart = pattern.substring(0, mandatoryEnd);
    boolean isFullPattern = mandatoryEnd == pattern.length();
    int currentIndex;
    if (mandatoryPatternPart.contains("<")) {
      currentIndex = parseArgs(args, input, mandatoryPatternPart, isFullPattern);
    } else {
      currentIndex = mandatoryEnd;
    }
    if (isFullPattern) {
      return new Arguments(args.build(), input);
    }

    String rawOptionalPatternPart = pattern.substring(mandatoryEnd);
    rawOptionalPatternPart = rawOptionalPatternPart.replace("[", "");
    String[] rawOptionalArgs = rawOptionalPatternPart.split("]");
    String optionalPatternPart = sortAndFilter(input.substring(currentIndex), rawOptionalArgs);
    if (mandatoryPatternPart.lastIndexOf(">") == mandatoryPatternPart.length() - 1) {
      int optionalPartFirstArgStart = optionalPatternPart.indexOf("<");
      optionalPartFirstArgStart = optionalPartFirstArgStart > 0 ? optionalPartFirstArgStart :
        optionalPatternPart.length();
      String lastMandatoryValue;
      if (optionalPartFirstArgStart != 0) {
        lastMandatoryValue = input.substring(currentIndex, input.indexOf(
          optionalPatternPart.substring(0, optionalPartFirstArgStart), currentIndex));
      } else {
        lastMandatoryValue = input.substring(currentIndex);
      }
      args.put(mandatoryPatternPart.substring(mandatoryPatternPart.lastIndexOf("<") + 1,
                                              mandatoryPatternPart.lastIndexOf(">")), lastMandatoryValue);
      currentIndex += lastMandatoryValue.length();
    }
    if (optionalPatternPart.contains("<")) {
      parseArgs(args, input.substring(currentIndex), optionalPatternPart, true);
    }
    return new Arguments(args.build(), input);
  }

  private int parseArgs(ImmutableMap.Builder<String, String> args, String input,
                    String pattern, boolean isFullPattern) {
    String[] patternArgs = pattern.split(">");
    int currentIndex = 0;
    for (int i = 0; i < patternArgs.length - 1; i++) {
      String[] currentOption = patternArgs[i].split("<");
      String[] nextOption = patternArgs[i + 1].split("<");
      if (!input.startsWith(currentOption[0], currentIndex)) {
        throw new IllegalArgumentException("Expected format: " + command.getPattern());
      }
      currentIndex += currentOption[0].length();
      int argEnd = input.indexOf(nextOption[0], currentIndex);
      if (argEnd == -1 || currentIndex == argEnd) {
        throw new IllegalArgumentException("Expected format: " + command.getPattern());
      }
      String value = input.substring(currentIndex, argEnd);
      args.put(currentOption[1], value);
      currentIndex += value.length();
    }
    currentIndex += patternArgs[patternArgs.length - 1].split("<")[0].length();
    if (isFullPattern) {
      if (currentIndex == input.length()) {
        throw new IllegalArgumentException("Expected format: " + command.getPattern());
      }
      args.put(patternArgs[patternArgs.length - 1].split("<")[1], input.substring(currentIndex));
    }
    return currentIndex;
  }

  private String sortAndFilter(final String input, String[] args) {
    String[] argsCopy = new String[args.length];
    System.arraycopy(args, 0, argsCopy, 0, args.length);
    Arrays.sort(argsCopy, new Comparator<String>() {

      @Override
      public int compare(String arg1, String arg2) {
        int arg2Length = input.indexOf(arg2.split("<")[0]);
        if (arg2Length == -1) {
          return -1;
        }
        int arg1Length = input.indexOf(arg1.split("<")[0]);
        if (arg1Length == -1) {
          return 1;
        }
        return arg1Length - arg2Length;
      }
    });
    StringBuilder builder = new StringBuilder();
    for (String arg : argsCopy) {
      if (!input.contains(arg.split("<")[0])) {
        break;
      }
      builder.append(arg);
    }
    return builder.toString();
  }
}
