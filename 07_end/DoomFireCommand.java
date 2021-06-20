///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1
//DEPS org.fusesource.jansi:jansi:2.3.2
//DEPS org.jline:jline:3.17.1
//SOURCES utils/JAnsiUtils.java utils/ImageUtils.java
//JAVA 11

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import picocli.CommandLine;
import picocli.CommandLine.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.fusesource.jansi.Ansi.ansi;

import utils.*;

/**
 *
 * Ansi Implementation of Doom Fire
 *
 * https://github.com/chriswhocodes/DoomFire
 */
@Command(helpCommand = true)
public class DoomFireCommand implements Callable<Void> {

  @Option(names = { "-l", "--logo" }, defaultValue = "logo.png")
  File logo;

  public static void main(String[] args) {

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      JAnsiUtils.showCursor(System.out, true);
    }));
    int exitCode = new CommandLine(new DoomFireCommand()).execute(args);
    System.exit(exitCode);
  }

  public boolean isDefaultColor(int color) {
    return color == 16 || color == 0;
  }

  @Override
  public Void call() throws Exception {
    JAnsiUtils.clearScreen(System.out);
    JAnsiUtils.showCursor(System.out, false);

    Terminal terminal = TerminalBuilder.builder().jansi(true).build();
    int screenHeight = (terminal.getHeight() - 5) * 2;
    int screenWidth = terminal.getWidth();

    int newLogoWidth = Math.round(screenWidth * 0.75f);

    DoomFire doomFire = new DoomFire(terminal, screenWidth, screenHeight);
    AnsiImage ansiImage = new AnsiImage(logo.toPath(), newLogoWidth);

    doomFire.setUp();

    int startLogoX = (screenWidth - newLogoWidth) / 2;
    int endLogoX = (screenWidth + newLogoWidth) / 2;

    int startLogoY = 10;
    int endLogoY = ansiImage.getHeight() + startLogoY - 1;

    int lastColor1 = 0;
    int lastColor2 = 0;
    while (true) {

      long startTime = System.currentTimeMillis();
      // compute fire
      doomFire.render();

      List<String> lines = new ArrayList<>();
      for (int y = 0; y < screenHeight - 1; y = y + 2) {

        AttributedStringBuilder line = new AttributedStringBuilder();
        for (int x = 0; x < screenWidth; x++) {

          // render 2 points
          int color1 = doomFire.getColor(x, y);
          int color2 = doomFire.getColor(x, y + 1);

          // logo position => replace fire color by the color from the image
          if (startLogoX < x && x < endLogoX && startLogoY < y && y < endLogoY) {
            int logoX = x - startLogoX;
            int logoY = y - startLogoY;
            int logoY1 = 1 + y - startLogoY;

            int colorLogo1 = ansiImage.getColor(logoX, logoY);
            int colorLogo2 = ansiImage.getColor(logoX, logoY1);

            if (!isDefaultColor(colorLogo1)) {
              color1 = colorLogo1;
            }

            if (!isDefaultColor(colorLogo2)) {
              color2 = colorLogo2;
            }
          }

          if (isDefaultColor(color1) && isDefaultColor(color2) && isDefaultColor(lastColor1)
              && isDefaultColor(lastColor2)) {
            line.append(' ');
          } else {
            // 2 points on 1 character
            line.style(AttributedStyle.DEFAULT.background(color1).foreground(color2)).append('▄');
          }

          lastColor1 = color1;
          lastColor2 = color2;

        }
        lines.add(line.toAnsi(terminal));
      }
      long renderTime = System.currentTimeMillis();
      lines.forEach(System.out::println);
      long printTime = System.currentTimeMillis();

      System.out.printf("render:%5d, print:%5d\n", (renderTime - startTime), (printTime - renderTime));

      System.out.print(ansi().cursorUpLine(screenHeight / 2 + 1));
      Thread.sleep(200);

    }

  }

  public static class DoomFire {

    private int imageWidth;
    private int imageHeight;

    private int[] palleteRefs;
    private int[] pixelData;

    private int[] pallete;
    Terminal terminal;

    public DoomFire(Terminal terminal, int imageWidth, int imageHeight) {
      this.terminal = terminal;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
    }

    public int getColor(int x, int y) {
      return pallete[palleteRefs[y * imageWidth + x]];
    }

    public void start() throws Exception {

      setUp();

      // int cpt = 0;
      JAnsiUtils.showCursor(System.out, false);
      while (true) {
        long startTime = System.currentTimeMillis();

        render();
        long startTime2 = System.currentTimeMillis();
        writeFireImage().forEach(System.out::println);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("duration total:  %5d, render:  %5d, write:  %5d\n", duration, startTime2 - startTime,
            endTime - startTime2);

        Thread.sleep(100);
        System.out.print(ansi().cursorUpLine(imageHeight / 2 + 1));
      }

    }

    private void setUp() {

      int pixelCount = imageWidth * imageHeight;

      palleteRefs = new int[pixelCount];

      pixelData = new int[pixelCount];

      createPallete();

      intialisePalleteRefs();
    }

    private void createPallete() {
      int[] rawPalleteRGB = new int[] { 0x00, 0x00, 0x00, 0x1F, 0x07, 0x07, 0x2F, 0x0F, 0x07, 0x47, 0x0F, 0x07, 0x57,
          0x17, 0x07, 0x67, 0x1F, 0x07, 0x77, 0x1F, 0x07, 0x8F, 0x27, 0x07, 0x9F, 0x2F, 0x07, 0xAF, 0x3F, 0x07, 0xBF,
          0x47, 0x07, 0xC7, 0x47, 0x07, 0xDF, 0x4F, 0x07, 0xDF, 0x57, 0x07, 0xDF, 0x57, 0x07, 0xD7, 0x5F, 0x07, 0xD7,
          0x5F, 0x07, 0xD7, 0x67, 0x0F, 0xCF, 0x6F, 0x0F, 0xCF, 0x77, 0x0F, 0xCF, 0x7F, 0x0F, 0xCF, 0x87, 0x17, 0xC7,
          0x87, 0x17, 0xC7, 0x8F, 0x17, 0xC7, 0x97, 0x1F, 0xBF, 0x9F, 0x1F, 0xBF, 0x9F, 0x1F, 0xBF, 0xA7, 0x27, 0xBF,
          0xA7, 0x27, 0xBF, 0xAF, 0x2F, 0xB7, 0xAF, 0x2F, 0xB7, 0xB7, 0x2F, 0xB7, 0xB7, 0x37, 0xCF, 0xCF, 0x6F, 0xDF,
          0xDF, 0x9F, 0xEF, 0xEF, 0xC7, 0xFF, 0xFF, 0xFF };

      int palleteSize = rawPalleteRGB.length / 3;

      pallete = new int[palleteSize];

      for (int i = 0; i < palleteSize; i++) {
        // int alpha = (i == 0) ? 0 : 255;

        int red = rawPalleteRGB[3 * i + 0];

        int green = rawPalleteRGB[3 * i + 1];

        int blue = rawPalleteRGB[3 * i + 2];

        int argb = org.jline.utils.Colors.roundRgbColor(red, green, blue, 256);

        pallete[i] = argb;
      }
    }

    private void intialisePalleteRefs() {
      int writeIndex = 0;

      for (int y = 0; y < imageHeight; y++) {
        for (int x = 0; x < imageWidth; x++) {
          if (y == imageHeight - 1) {
            palleteRefs[writeIndex++] = pallete.length - 1;
          } else {
            palleteRefs[writeIndex++] = 0;
          }
        }
      }
    }

    public void render() {
      for (int x = 0; x < imageWidth; x++) {
        for (int y = imageHeight - 1; y > 0; y--) {
          spreadFire(y * imageWidth + x);
        }
      }
    }

    private void spreadFire(int src) {
      int rand = (int) Math.round(Math.random() * 3.0) & 3;
      int dst = src - rand + 1;
      palleteRefs[Math.max(0, dst - imageWidth)] = Math.max(0, palleteRefs[src] - (rand & 1));
    }

    private List<String> writeFireImage() {
      List<String> lines = new ArrayList<>();
      for (int y = 0; y < imageHeight - 1; y = y + 2) {
        AttributedStringBuilder line = new AttributedStringBuilder();
        for (int x = 0; x < imageWidth; x++) {
          int color1 = getColor(y, x);
          int color2 = getColor(y + 1, x);
          line.style(AttributedStyle.DEFAULT.background(color1).foreground(color2)).append('▄');
        }
        lines.add(line.toAnsi(terminal));
      }
      return lines;
    }
  }

  public static class AnsiImage {
    BufferedImage image;

    public AnsiImage(Path path, int width) throws IOException {
      BufferedImage logoImage = ImageIO.read(path.toFile());
      int newLogoHeight = Long.valueOf(Math.round(width * logoImage.getHeight() / logoImage.getWidth())).intValue();
      image = ImageUtils.resize(logoImage, width, newLogoHeight);
    }

    public int getWidth() {
      return image.getWidth();
    }

    public int getHeight() {
      return image.getHeight();
    }

    public int getColor(int x, int y) {
      int pixel1 = image.getRGB(x, y);
      return ImageUtils.rgbToAnsiColor(pixel1);
    }

  }

}
