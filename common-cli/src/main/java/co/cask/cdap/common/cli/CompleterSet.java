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

package co.cask.cdap.common.cli;

import com.google.common.collect.ImmutableMap;
import jline.console.completer.Completer;

import java.util.Map;

/**
 * Set of {@link Completer}s.
 *
 * {@link #completers} is a {@link Map} of argument name to {@link Completer}. When the user
 * requests auto-completion for an argument, the {@link CLI} checks if there is a {@link Completer}
 * for that argument and uses it if available.
 */
public class CompleterSet {

  private final Map<String, Completer> completers;

  /**
   * @param completers {@link Map} of argument name to {@link Completer}.
   */
  public CompleterSet(Map<String, Completer> completers) {
    this.completers = ImmutableMap.copyOf(completers);
  }

  /**
   * @return {@link Map} of argument name to {@link Completer}.
   */
  public Map<String, Completer> getCompleters() {
    return completers;
  }

  /**
   * @param type the argument name
   * @return the completer associated with the provided type
   */
  public Completer getCompleter(String type) {
    return completers.get(type);
  }
}
