package utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class ITerm2Utils {

    final static char ESC = 0x1b; // ^[
    final static char REST = 0x07; // ^G

    private static String esc(String cmd) {
        return ESC + cmd + REST;
    }

    private static String color(String n, String rrggbbColor) {
        return esc("]P" + n + rrggbbColor);
    }

    public static String foreground(String rrggbbColor) {
        return color("g", rrggbbColor);
    }

    public static String background(String rrggbbColor) {
        return color("h", rrggbbColor);
    }

    public static String clearScrollbackHistory() {
        return esc("]1337;ClearScrollback");
    }

    public static String link(String url, String text) {
        return esc("]8;;" + url + REST + text + ESC + "]8;;");
    }

    /**
     * ITerm wrap File capability imgcat
     * https://www.iterm2.com/documentation-images.html
     */
    public static String file(byte[] content, boolean download, Optional<Long> width, Optional<Long> height,
                              Optional<String> name) {
        final List<String> buff = new ArrayList<>();

        width.ifPresent(s -> buff.add("width=" + s));
        height.ifPresent(s -> buff.add("height=" + s));
        name.ifPresent(s -> buff.add("name=" + s));
        buff.add(download ? "inline=0" : "inline=1");
        if (download) {
            buff.add("size=" + content.length);
        }

        var params = String.join(";", buff);
        var b64 = Base64.getEncoder().encodeToString(content);
        return esc("]1337;File=" + params + " :" + b64);

    }
}
