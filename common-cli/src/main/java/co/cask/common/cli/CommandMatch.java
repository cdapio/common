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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents an input matching for a command and provided arguments.
 */
public final class CommandMatch {
  
  private static final char MANDATORY_ARG_BEGINNING = '<';
  private static final char MANDATORY_ARG_ENDING = '>';
  private static final char OPTIONAL_PART_BEGINNING = '[';
  private static final char OPTIONAL_PART_ENDING = ']';

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
    return parseArguments(input.trim(), command.getPattern());
  }

  public String getInput() {
    return input;
  }

  /**
   * Parse arguments from the input and command pattern.
   *
   * @param input the input
   * @param pattern the command pattern
   * @return parsed arguments
   */
  private Arguments parseArguments(String input, String pattern) {
    ImmutableMap.Builder<String, String> args = ImmutableMap.builder();

    List<String> splitInput = Parser.parseInput(input);
    List<String> splitPattern = Parser.parsePattern(pattern);

    while (!splitInput.isEmpty()) {
      if (splitPattern.isEmpty()) {
        throw new IllegalArgumentException("Expected format: " + command.getPattern());
      }
      String patternPart = splitPattern.get(0);
      String inputPart = splitInput.get(0);
      if (patternPart.startsWith((Character.toString(OPTIONAL_PART_BEGINNING))) &&
          patternPart.endsWith((Character.toString(OPTIONAL_PART_ENDING)))) {
        args.putAll(parseOptional(splitInput, getEntry(patternPart)));
      } else {
        if (patternPart.startsWith((Character.toString(MANDATORY_ARG_BEGINNING))) &&
            patternPart.endsWith((Character.toString(MANDATORY_ARG_ENDING)))) {
          args.put(getEntry(patternPart), inputPart);
        } else if (!patternPart.equals(inputPart)) {
          throw new IllegalArgumentException("Expected format: " + command.getPattern());
        }
        splitInput.remove(0);
      }
      splitPattern.remove(0);
    }
    return new Arguments(args.build(), input);
  }

  /**
   * Parse arguments from the split input and pattern.
   * Used for parsing optional parameters. Does not cause the effect in case specified parameter absent.
   *
   * @param splitInput the split input
   * @param pattern the pattern
   * @return the map of arguments
   */
  private Map<String, String> parseOptional(List<String> splitInput, String pattern) {
    ImmutableMap.Builder<String, String> args = ImmutableMap.builder();

    List<String> copyInput = new ArrayList<String>(splitInput);
    List<String> splitPattern = Parser.parsePattern(pattern);

    while (!splitPattern.isEmpty()) {
      if (copyInput.isEmpty()) {
        return Collections.emptyMap();
      }
      String patternPart = splitPattern.get(0);
      String inputPart = copyInput.get(0);
      if (patternPart.startsWith((Character.toString(MANDATORY_ARG_BEGINNING))) &&
          patternPart.endsWith((Character.toString(MANDATORY_ARG_ENDING)))) {
        args.put(getEntry(patternPart), inputPart);
      } else if (patternPart.startsWith((Character.toString(OPTIONAL_PART_BEGINNING))) &&
                 patternPart.endsWith((Character.toString(OPTIONAL_PART_ENDING)))) {
        args.putAll(parseOptional(copyInput, getEntry(patternPart)));
      } else if (!patternPart.equals(inputPart)) {
        return Collections.emptyMap();
      }
      splitPattern.remove(0);
      copyInput.remove(0);
    }

    splitInput.clear();
    splitInput.addAll(copyInput);
    return args.build();
  }

  /**
   * Retrieves entry from input {@link String}.
   * For example, for input "<some input>" returns "some input".
   *
   * @param input the input
   * @return entry {@link String}
   */
  private String getEntry(String input) {
    return input.substring(1, input.length() - 1);
  }

  /**
   * Utility class for parsing input and pattern
   */
  public static class Parser {

    private static final char SEPARATOR = ' ';
    private static final char ARG_WRAPPER = '"';
    private static final char JSON_WRAPPER = '\'';

    private static enum State {
      EMPTY, IN_QUOTES, IN_DOUBLE_QUOTES, IN_MANDATORY_ARG, IN_OPTIONAL_PART
    }

    /**
     * Parse input. Split input {@link String} into items.
     * Each item is a word, or some expression that starts with """ and ends with """,
     *  or starts with "'" and ends with "'".
     *
     * @param input the input
     * @return parsed input
     */
    public static List<String> parseInput(String input) {
      List<String> splitInput = new ArrayList<String>();
      StringBuilder builder = new StringBuilder();
      State state = State.EMPTY;
      for (char ch : input.toCharArray()) {
        switch (state) {
          case EMPTY:
            if (ch == SEPARATOR) {
              splitInput.add(builder.toString());
              builder.setLength(0);
              break;
            }
            if (ch == ARG_WRAPPER) {
              state = State.IN_DOUBLE_QUOTES;
            }
            if (ch == JSON_WRAPPER) {
              state = State.IN_QUOTES;
            }
            builder.append(ch);
            break;
          case IN_DOUBLE_QUOTES:
            if (ch == ARG_WRAPPER) {
              state = State.EMPTY;
            }
            builder.append(ch);
            break;
          case IN_QUOTES:
            if (ch == JSON_WRAPPER) {
              state = State.EMPTY;
            }
            builder.append(ch);
            break;
        }
      }
      if (builder.length() > 0) {
        splitInput.add(builder.toString());
      }
      return splitInput;
    }

    /**
     * Parse pattern. Split pattern {@link String} into items.
     * Each item is a word, or some expression that starts with "<" and ends with ">",
     *  or starts with "[" and ends with "]".
     *
     * @param pattern the pattern
     * @return parsed pattern
     */
    public static List<String> parsePattern(String pattern) {
      List<String> splitPattern = new ArrayList<String>();
      StringBuilder builder = new StringBuilder();
      State state = State.EMPTY;
      for (char ch : pattern.toCharArray()) {
        switch (state) {
          case EMPTY:
            if (ch == SEPARATOR) {
              splitPattern.add(builder.toString());
              builder.setLength(0);
              break;
            }
            if (ch == MANDATORY_ARG_BEGINNING) {
              state = State.IN_MANDATORY_ARG;
            }
            if (ch == OPTIONAL_PART_BEGINNING) {
              state = State.IN_OPTIONAL_PART;
            }
            builder.append(ch);
            break;
          case IN_MANDATORY_ARG:
            if (ch == MANDATORY_ARG_ENDING) {
              state = State.EMPTY;
            }
            builder.append(ch);
            break;
          case IN_OPTIONAL_PART:
            if (ch == OPTIONAL_PART_ENDING) {
              state = State.EMPTY;
            }
            builder.append(ch);
            break;
        }
      }
      if (builder.length() > 0) {
        splitPattern.add(builder.toString());
      }
      return splitPattern;
    }
  }
}
