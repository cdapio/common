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
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Completer for a set of strings.
 */
public abstract class StringsCompleter extends AbstractCompleter {

  protected abstract Supplier<Collection<String>> getStringsSupplier();

  @Override
  protected Collection<String> getAllCandidates() {
    return getStrings();
  }

  @Override
  protected Collection<String> getCandidates(String buffer) {
    List<String> mutableCandidates = Lists.newArrayList();
    for (String candidate : getStrings()) {
      if (candidate.startsWith(buffer)) {
        mutableCandidates.add(candidate);
      }
    }
    Collections.sort(mutableCandidates, Ordering.usingToString());
    return Collections.unmodifiableList(mutableCandidates);
  }

  public Collection<String> getStrings() {
    return getStringsSupplier().get();
  }
}
