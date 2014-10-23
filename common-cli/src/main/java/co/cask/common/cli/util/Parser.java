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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing input and pattern
 */
public class Parser {

  public static final char MANDATORY_ARG_BEGINNING = '<';
  public static final char MANDATORY_ARG_ENDING = '>';
  public static final char OPTIONAL_PART_BEGINNING = '[';
  public static final char OPTIONAL_PART_ENDING = ']';

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
