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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import jline.console.completer.Completer;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link co.cask.common.cli.completers.DefaultStringsCompleter}.
 */
public class CompleterTestBase {

  private static final Gson GSON = new Gson();

  protected void testCompleter(Completer completer, String buffer, int cursor, List<CharSequence> expectedCandidates) {
    List<CharSequence> candidates = Lists.newArrayList();
    completer.complete(buffer, cursor, candidates);
    Assert.assertEquals(getCompleterFailMessage(expectedCandidates, candidates),
                        expectedCandidates.size(), candidates.size());
    for (CharSequence expectedCandidate : expectedCandidates) {
      Assert.assertTrue(getCompleterFailMessage(expectedCandidates, candidates),
                        candidates.contains(expectedCandidate));
    }
  }

  protected void testOrder(Completer completer, String buffer, int cursor, List<CharSequence> expectedCandidates) {
    List<CharSequence> candidates = Lists.newArrayList();
    completer.complete(buffer, cursor, candidates);
    Assert.assertEquals(candidates, expectedCandidates);
  }

  private String getCompleterFailMessage(List<CharSequence> expectedCandidates, List<CharSequence> candidates) {
    return "Expected candidates " + GSON.toJson(expectedCandidates) + ", but got " + GSON.toJson(candidates);
  }

}
