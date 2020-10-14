package org.orthomcl.service.core.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RenderingHelper {

  public static <T extends Renderable> void assignSpectrumColors(List<T> renderables) {
    // determine the color of each renderable item in the spectrum, and color is scaled from 360 degree to
    // 256 * 6 space.
    for (int i = 0; i < renderables.size(); i++) {
      int color = Math.round(i * 256 * 6F / renderables.size());
      int range = color / 256;
      int subRange = color % 256;
      String colorCode;
      if (range == 0 || range == 6) { // on blue, increasing green
        colorCode = "#00" + toHex(subRange) + "FF";
      }
      else if (range == 1) { // on green, decreasing blue
        colorCode = "#00FF" + toHex(255 - subRange);
      }
      else if (range == 2) { // on green, increasing red
        colorCode = "#" + toHex(subRange) + "FF00";
      }
      else if (range == 3) { // on red, decreasing green
        colorCode = "#FF" + toHex(255 - subRange) + "00";
      }
      else if (range == 4) { // on red, increasing blue
        colorCode = "#FF00" + toHex(subRange);
      }
      else { // on blue, decreasing red
        colorCode = "#" + toHex(255 - subRange) + "00FF";
      }
      renderables.get(i).setColor(colorCode);
    }
  }

  public static <T extends Renderable> void assignRandomColors(Collection<T> renderables) {
    // always use a fixed seeder, so that the same taxon will always get the same color;
    Random random = new Random(0);
    int step = 255 * 3 / renderables.size() + 1;
    List<String> colors = new ArrayList<>();
    while (colors.size() < renderables.size()) {
      step--;
      if (step == 0)
        break;
      for (int r = 0; r < 256; r += step) {
        for (int g = 0; g < 256; g += step) {
          for (int b = 0; b < 256; b += step) {
            String color = "#" + toHex(r) + toHex(g) + toHex(b);
            colors.add(color);
          }
        }
      }
    }

    Collections.shuffle(colors, random);
    int i = 0;
    for (Renderable renderable : renderables) {
      if (i < colors.size()) {
        renderable.setColor(colors.get(i));
        i++;
      }
    }
  }

  public static String toHex(int c) {
    c = c % 256;
    String hex = Integer.toHexString(c);
    if (hex.length() == 1)
      hex = "0" + hex;
    return hex;
  }

}
