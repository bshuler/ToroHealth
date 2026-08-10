package net.torocraft.torohealth.config.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DefaulterTest {

  public static class Nested {
    public String name = "default-name";
    public int count = 5;
  }

  public static class Outer {
    public String label = "default-label";
    public Nested nested = new Nested();
  }

  // No public no-arg constructor -- exercises hasDefaultConstructor()'s
  // catch(Exception) branch (NoSuchMethodException) via a normal field scan,
  // and setDefaults() must simply skip recursing into it rather than fail.
  public static class NoDefaultConstructor {
    public final int value;

    public NoDefaultConstructor(int value) {
      this.value = value;
    }
  }

  public static class HasFieldWithNoDefaultConstructor {
    public String label = "default-label";
    public NoDefaultConstructor bad = new NoDefaultConstructor(1);
  }

  @Test
  void setDefaults_fillsNullTopLevelFieldFromAFreshDefaultInstance() {
    Outer o = new Outer();
    o.label = null;
    o.nested = null;

    Defaulter.setDefaults(o, Outer.class);

    assertEquals("default-label", o.label);
    assertNotNull(o.nested);
  }

  @Test
  void setDefaults_recursesIntoNestedObjectFieldsThatHaveADefaultConstructor() {
    Outer o = new Outer();
    o.nested.name = null;

    Defaulter.setDefaults(o, Outer.class);

    assertEquals("default-name", o.nested.name);
    assertEquals(5, o.nested.count, "primitive fields are already non-null and left untouched");
  }

  @Test
  void setDefaults_leavesNonNullTopLevelFieldsUntouched() {
    Outer o = new Outer();
    o.label = "custom";

    Defaulter.setDefaults(o, Outer.class);

    assertEquals("custom", o.label);
  }

  @Test
  void setDefaults_skipsRecursionIntoFieldsWhoseTypeHasNoDefaultConstructor() {
    HasFieldWithNoDefaultConstructor o = new HasFieldWithNoDefaultConstructor();

    assertDoesNotThrow(() -> Defaulter.setDefaults(o, HasFieldWithNoDefaultConstructor.class));

    assertEquals("default-label", o.label);
    assertEquals(1, o.bad.value, "the un-defaultable nested object itself must be left alone");
  }

  @Test
  void setDefaults_swallowsFailureWhenTheGivenClassHasNoDefaultConstructorItself() {
    NoDefaultConstructor o = new NoDefaultConstructor(7);

    // clazz.getDeclaredConstructor().newInstance() throws before touching o;
    // the outer catch(Exception) must swallow it rather than propagate.
    assertDoesNotThrow(() -> Defaulter.setDefaults(o, NoDefaultConstructor.class));

    assertEquals(7, o.value, "no field should have been touched");
  }

  @Test
  void setDefaults_fillsNullFieldFromDefaultsEvenWhenItsOwnTypeIsNotRecursable() {
    // hasDefaultConstructor() only gates *recursion* into a field's value; a
    // null field is still filled from the fresh default instance's value
    // regardless of whether that value's own type has a default constructor.
    HasFieldWithNoDefaultConstructor o = new HasFieldWithNoDefaultConstructor();
    o.bad = null;

    assertDoesNotThrow(() -> Defaulter.setDefaults(o, HasFieldWithNoDefaultConstructor.class));

    assertNotNull(o.bad);
    assertEquals(1, o.bad.value);
  }

  @Test
  void constructor_isPubliclyInstantiableLikeAnyOtherClass() {
    // Defaulter exposes only static methods and declares no constructor of
    // its own, so javac emits a plain public implicit no-arg one -- exercise
    // it directly (no reflection) so it isn't a permanent JaCoCo gap.
    assertNotNull(new Defaulter());
  }
}
