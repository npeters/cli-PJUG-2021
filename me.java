///usr/bin/env jbang "$0" "$@" ; exit $?
// //DEPS <dependency1> <dependency2>
//SOURCES 03_term/utils/ITerm2Utils.java 03_term/utils/JAnsiUtils.java

//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1
//DEPS org.fusesource.jansi:jansi:2.3.2
//DEPS org.jline:jline:3.17.1
//JAVA 16

import static java.lang.System.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import utils.ITerm2Utils;
import utils.JAnsiUtils;

import static utils.JAnsiUtils.*;

public class me {

    public static void main(String... args) throws Exception {
        utils.ITerm2Utils.clearScrollbackHistory();
        clearScreen(out);

        Terminal terminal = TerminalBuilder.builder().dumb(true).build();

        int col = terminal.getSize().getColumns();

        printlnList(center(List.of(//
                text("@|bold    JAVA CLI|@"), //
                text("@|bold     ====================== |@")), //
                col));

        out.println(" ".repeat(((col / 2) - 14)) + ITerm2Utils.file(Files.readAllBytes(Path.of("images","profile.png")),
                false, Optional.empty(), Optional.empty(), Optional.empty()));

        printlnList(center( //
                List.of( //
                        text("@|bold     Nicolas Peters |@"), //
                        text("@|bold       CANAL+ |@")),
                col));
        out.println();

        System.in.read();
        clearScreen(out);
    }
}
