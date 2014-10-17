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

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/**
 * Test for {@link PrefixCompleter}.
 */
public class PrefixCompleterTest extends CompleterTestBase {

  @Test
  public void testComplete() {
    DefaultStringsCompleter childCompleter = new DefaultStringsCompleter(
      ImmutableList.<CharSequence>of("asdf", "asdd", "bdf"));
    PrefixCompleter completer = new PrefixCompleter("asd lkj", childCompleter);

    testCompleter(completer, "asd lkj a", 0, ImmutableList.<CharSequence>of("asdf", "asdd"));
    testCompleter(completer, "asd lkj b", 0, ImmutableList.<CharSequence>of("bdf "));
    testCompleter(completer, "asd lkj c", 0, ImmutableList.<CharSequence>of());

    testCompleter(completer, "asd a", 0, ImmutableList.<CharSequence>of());
    testCompleter(completer, "asd b", 0, ImmutableList.<CharSequence>of());
    testCompleter(completer, "asd c", 0, ImmutableList.<CharSequence>of());

    testCompleter(completer, "a", 0, ImmutableList.<CharSequence>of());
    testCompleter(completer, "b", 0, ImmutableList.<CharSequence>of());
    testCompleter(completer, "c", 0, ImmutableList.<CharSequence>of());
  }

}
