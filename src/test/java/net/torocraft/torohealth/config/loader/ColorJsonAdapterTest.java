package net.torocraft.torohealth.config.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class ColorJsonAdapterTest {

  private final ColorJsonAdapter adapter = new ColorJsonAdapter();

  private String write(int value) throws Exception {
    StringWriter out = new StringWriter();
    try (JsonWriter writer = new JsonWriter(out)) {
      adapter.write(writer, value);
    }
    return out.toString();
  }

  private Integer read(String json) throws Exception {
    try (JsonReader reader = new JsonReader(new StringReader(json))) {
      return adapter.read(reader);
    }
  }

  @Test
  void write_formatsRedAsLowercaseSixDigitHexWithHashPrefix() throws Exception {
    assertEquals("\"#ff0000\"", write(0xff0000));
  }

  @Test
  void write_padsShortHexValuesWithLeadingZeros() throws Exception {
    assertEquals("\"#0000ff\"", write(0x0000ff));
    assertEquals("\"#000000\"", write(0));
  }

  @Test
  void write_masksOffAnyBitsAboveTheLow24() throws Exception {
    // Mirrors how a java.awt.Color-derived getRGB() int would come in, with
    // an alpha byte set in the high byte.
    assertEquals("\"#ff0000\"", write(0xffff0000));
  }

  @Test
  void read_parsesHexStringBackToTheSameIntWriteProduced() throws Exception {
    assertEquals(0xff0000, read("\"#ff0000\""));
    assertEquals(0x00ff00, read("\"#00ff00\""));
    assertEquals(0x123456, read("\"#123456\""));
  }

  @Test
  void writeThenRead_roundTripsToTheOriginalValue() throws Exception {
    int original = 0x00ff00;
    String json = write(original);
    assertEquals(original, read(json));
  }

  @Test
  void read_returnsNullAndDoesNotThrowWhenValueIsNotAValidColor() throws Exception {
    Integer result = read("\"not-a-color\"");
    assertNull(result);
  }
}
