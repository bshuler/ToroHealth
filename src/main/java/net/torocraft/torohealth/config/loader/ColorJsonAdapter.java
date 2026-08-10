package net.torocraft.torohealth.config.loader;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.awt.Color;
import java.io.IOException;

public class ColorJsonAdapter extends TypeAdapter<Integer> {

  @Override
  public void write(JsonWriter out, Integer value) throws IOException {
    String hex = Integer.toHexString(value & 0xffffff);
    hex = String.format("#%1$6s", hex).replace(' ', '0');
    out.value(hex);
  }

  @Override
  public Integer read(JsonReader in) throws IOException {
    String read = in.nextString();
    try {
      Color c = Color.decode(read);
      // Color.getRGB() sets the high byte to a fully-opaque alpha (0xff), but
      // write() above only ever emits the low 24 RGB bits with no alpha
      // component -- mask it back off here so read(write(x)) round-trips to
      // the same int write() was given instead of x | 0xff000000 (a real bug
      // found while adding test coverage; see PLAN.md "Phase 2").
      return c.getRGB() & 0xffffff;
    } catch (Exception e) {
      System.out.println("ToroHealth: failed to parse color [" + read + "]");
      e.printStackTrace();
      return null;
    }
  }

}
