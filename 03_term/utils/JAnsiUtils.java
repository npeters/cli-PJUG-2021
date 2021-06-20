package utils ;
import picocli.CommandLine;

import java.io.PrintStream;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;

import static org.jline.utils.WCWidth.wcwidth;

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

    public static String text(String txt) {
        return CommandLine.Help.Ansi.AUTO.text(txt).toString();
    }

    public static void printBox(int screenWidth, String ansi) {
        printlnList(center(box(ansi), screenWidth));
    }

    public  static void printlnList(Collection<String> lines) {
        lines.forEach(System.out::println);

    }

    public static List<String> center(List<String> lines, int width) {

        return lines.stream().map(line -> {
            int left = (width - line.length()) / 2;
            //int rigth = (width - max - line.length()) - left;
            return " ".repeat(left) + line ;
        }).toList();
    }

    static int getStringDisplayLength(String s) {
        int displayWidth = 0;
        for (int i = 0; i < s.length(); i++)
            displayWidth += Math.max(wcwidth(s.charAt(i)), 0);
        return displayWidth;
    }

    public static List<String> box(String message) {
        return box(message, Math.round(getStringDisplayLength(message) + 10));
    }

    public static List<String> box(String message, int width) {

        int length = getStringDisplayLength(message);
        var w = Math.max(width, length);

        var margingLeft = ((w - length) + 2) / 2;
        var marginRigth = 2 + w - margingLeft - length;

        return List.of( //
                "┌" + "─".repeat(w + 2) + ("┐"), //
                "│" + " ".repeat(margingLeft) + message + " ".repeat(marginRigth) + "│", //
                "└" + "─".repeat(w + 2) + "┘" //
        );
    }

}

