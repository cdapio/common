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

package io.cdap.common.cli.supplier;

import io.cdap.common.cli.completers.PrefixCompleter;
import jline.console.completer.Completer;

/**
 * Default implementation {@link CompleterSupplier}.
 */
public class DefaultCompleterSupplier implements CompleterSupplier {

  @Override
  public Completer getCompleter(String prefix, Completer completer) {
    if (prefix != null && !prefix.isEmpty()) {
      return new PrefixCompleter(prefix, completer);
    } else {
      return completer;
    }
  }
}
