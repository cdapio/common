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

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Completer for a set of strings.
 */
public abstract class StringsCompleter extends AbstractCompleter {

  protected abstract Supplier<List<? extends CharSequence>> getStringsSupplier();

  @Override
  protected List<? extends CharSequence> getAllCandidates() {
    return getStrings();
  }

  @Override
  protected List<? extends CharSequence> getCandidates(String buffer) {
    List<CharSequence> candidates = Lists.newArrayList();

    for (CharSequence candidate : getStrings()) {
      if (candidate.toString().startsWith(buffer)) {
        candidates.add(candidate);
      }
    }

    return candidates;
  }

  public List<? extends CharSequence> getStrings() {
    return getStringsSupplier().get();
  }
}
