/*
 * Copyright © 2014 Cask Data, Inc.
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

package io.cdap.common.io;

import com.google.common.reflect.TypeToken;
import io.cdap.common.internal.io.ASMFieldAccessorFactory;
import io.cdap.common.internal.io.FieldAccessor;
import io.cdap.common.internal.io.FieldAccessorFactory;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class FieldAccessorTest {

  /**
   * Test parent.
   * @param <T>
   */
  public static class Parent<T> {
    private T value;
    private boolean b;
  }

  /**
   * Test child.
   */
  public static class Child extends Parent<String> {
    protected String str;
    int integer;
  }

  @Test
  public void testGetter() {
    TypeToken<Child> type = TypeToken.of(Child.class);

    FieldAccessorFactory factory = new ASMFieldAccessorFactory();
    FieldAccessor accessor = factory.getFieldAccessor(type, "integer");

    Assert.assertSame(accessor, factory.getFieldAccessor(type, "integer"));

    Child c = new Child();
    c.integer = 10;
    c.str = "child value";
    ((Parent<String>) c).value = "string value";
    ((Parent<String>) c).b = true;

    Assert.assertEquals(c.integer, accessor.getInt(c));
    Assert.assertSame(c.str, factory.getFieldAccessor(type, "str").get(c));
    Assert.assertSame(((Parent) c).value, factory.getFieldAccessor(type, "value").get(c));
    Assert.assertEquals(((Parent) c).b, factory.getFieldAccessor(type, "b").get(c));
  }

  @Test
  public void testSetter() {
    TypeToken<Child> type = TypeToken.of(Child.class);

    FieldAccessorFactory factory = new ASMFieldAccessorFactory();

    Child c = new Child();
    c.integer = 10;
    c.str = "child value";
    ((Parent<String>) c).value = "string value";
    ((Parent<String>) c).b = true;

    Child c2 = new Child();

    factory.getFieldAccessor(type, "integer").setInt(c2, c.integer);
    factory.getFieldAccessor(type, "str").set(c2, c.str);
    factory.getFieldAccessor(type, "value").set(c2, ((Parent) c).value);
    factory.getFieldAccessor(type, "b").setBoolean(c2, ((Parent) c).b);

    Assert.assertEquals(c.integer, c2.integer);
    Assert.assertSame(c.str, c2.str);
    Assert.assertSame(((Parent) c).value, ((Parent) c2).value);
    Assert.assertEquals(((Parent) c).b, ((Parent) c2).b);
  }

  @Test
  public void testReflectionFieldExceptionHandling() throws Exception {
    TypeToken<Child> type = TypeToken.of(Child.class);
    FieldAccessorFactory factory = new ASMFieldAccessorFactory();
    FieldAccessor accessor = factory.getFieldAccessor(type, "value");

    // Exercise the catch handler in initializeReflectionField (constructor) when field lookup fails
    try {
      accessor.getClass().getConstructor(TypeToken.class).newInstance(TypeToken.of(Object.class));
      Assert.fail("Expected InvocationTargetException wrapping RuntimeException");
    } catch (InvocationTargetException e) {
      Assert.assertTrue(e.getCause() instanceof RuntimeException);
      Assert.assertTrue(e.getCause().getCause() instanceof NoSuchFieldException);
    }

    // Exercise the IllegalAccessException catch handler in invokeReflection (get and set)
    Field accessorField = accessor.getClass().getDeclaredField("field");
    accessorField.setAccessible(true);
    Field targetField = (Field) accessorField.get(accessor);
    targetField.setAccessible(false);
    try {
      Child c = new Child();
      try {
        accessor.get(c);
        Assert.fail("Expected RuntimeException wrapping IllegalAccessException on get");
      } catch (RuntimeException e) {
        Assert.assertTrue(e.getCause() instanceof IllegalAccessException);
      }

      try {
        accessor.set(c, "new value");
        Assert.fail("Expected RuntimeException wrapping IllegalAccessException on set");
      } catch (RuntimeException e) {
        Assert.assertTrue(e.getCause() instanceof IllegalAccessException);
      }
    } finally {
      targetField.setAccessible(true);
    }
  }
}
