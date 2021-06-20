package utils;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageUtils {
  static Map<Integer, Integer> colorCache = new ConcurrentHashMap<>();

  public static int rgbToAnsiColor(int rgbColor) {
    return colorCache.computeIfAbsent(rgbColor, (Integer rc) -> {
      Color srcColor = new Color(rc, true);
      return org.jline.utils.Colors.roundRgbColor(srcColor.getRed(), srcColor.getGreen(), srcColor.getBlue(), 256);
    });
  }

  public static BufferedImage resize(BufferedImage img, int width, int height) {
    Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = resized.createGraphics();
    g2d.drawImage(tmp, 0, 0, null);
    g2d.dispose();
    return resized;
  }

  public static void printImage(Terminal terminal, BufferedImage image) {
    int h = image.getHeight();
    int w = image.getWidth();

    for (int y = 0; y < h - 1; y = y + 2) {
      AttributedStringBuilder line = new AttributedStringBuilder();
      for (int x = 0; x < w; x++) {
        int pixel1 = image.getRGB(x, y);
        int pixel2 = image.getRGB(x, y + 1);
        int colorId1 = rgbToAnsiColor(pixel1);
        int colorId2 = rgbToAnsiColor(pixel2);

        line.style(AttributedStyle.DEFAULT.background(colorId1).foreground(colorId2)).append('▄');

      }
      System.out.println(line.toAnsi(terminal));
    }
  }

}
