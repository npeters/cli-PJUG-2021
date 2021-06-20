package utils;

import java.io.PrintStream;

import static org.fusesource.jansi.Ansi.ansi;

public class JAnsiUtils {
  public static void hrAndUp(int n) {
    hr(n);
    System.out.println(ansi().cursorUpLine(n));
  }

  public static void hr(int n) {
    for (int i = 0; i < n; i++) {
      System.out.println();
    }
  }

  public static void clearScreen(PrintStream out) {
    out.print(ansi().cursor(0, 0).eraseScreen());
  }

  public static void showCursor(PrintStream out, boolean show) {
    if (show) {
      out.print("\033[?12l\033[?25h");
    } else {
      out.print("\033[?25l");
    }
  }
}