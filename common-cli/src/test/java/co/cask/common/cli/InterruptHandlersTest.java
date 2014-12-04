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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

public class InterruptHandlersTest {
  private boolean isUserInterruptHandled;

  @Test
  public void test() throws Exception {
    CLI cli = new CLI(Collections.emptyList(), Collections.emptyMap());
    char ctrlBreak = (char) 3;
    InputStream inputStream = new ByteArrayInputStream(String.valueOf(ctrlBreak).getBytes());

    Field reader = cli.getClass().getDeclaredField("reader");
    reader.setAccessible(true);
    Method setInput = reader.getType().getDeclaredMethod("setInput", InputStream.class);
    setInput.setAccessible(true);
    setInput.invoke(cli.getReader(), inputStream);

    cli.addUserInterruptHandler(getUserInterruptHandler());
    cli.startInteractiveMode(System.out);
    Assert.assertTrue(isUserInterruptHandled);
  }

  private CLI.UserInterruptHandler getUserInterruptHandler() {
    return new CLI.UserInterruptHandler() {
      @Override
      public void onUserInterrupt() throws IOException {
        isUserInterruptHandled = true;
      }
    };
  }
}
