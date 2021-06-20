///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.guava:guava:30.1.1-jre
//DEPS com.pivovarit:throwing-function:1.5.1

//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1
//DEPS org.fusesource.jansi:jansi:2.3.2
//DEPS org.jline:jline:3.17.1

// ProgressBar https://tongfei.me/progressbar/
//DEPS me.tongfei:progressbar:0.9.1

// Table
//DEPS de.vandermeer:asciitable:0.3.2
//DEPS com.jakewharton.fliptables:fliptables:1.1.0

// Graph
//DEPS com.mitchtalmadge:ascii-data:1.4.0

//SOURCES utils/JAnsiUtils.java utils/ITerm2Utils.java

//JAVA 16

import com.google.common.collect.Streams;
import com.jakewharton.fliptables.FlipTableConverters;
import com.mitchtalmadge.asciidata.graph.ASCIIGraph;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciithemes.TA_GridThemes;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import utils.ITerm2Utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.pivovarit.function.ThrowingConsumer.unchecked;
import static java.lang.System.in;
import static java.lang.System.out;
import static org.fusesource.jansi.Ansi.ansi;
import static utils.JAnsiUtils.*;

@Command(name = "TermDisplay", mixinStandardHelpOptions = true, version = "TermDisplay 0.1", description = "TermDisplay made with jbang")
class TermDisplay implements Callable<Integer> {

    @CommandLine.Option(names = "-f", defaultValue = "data.json")
    String filePath;

    public static void main(String... args) {
        int exitCode = new CommandLine(new TermDisplay()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        out.println(ITerm2Utils.clearScrollbackHistory());

        final int screenWidth = getScreenWidth();

        List<Consumer<Integer>> pages = List.of(this::ansiColor, unchecked(this::ansiMove), unchecked(this::spinner),
                unchecked(this::progessbar), unchecked(this::tableStringFormat), unchecked(this::tableFliptables),
                unchecked(this::tableAsciitable), unchecked(this::graph), unchecked(this::jline3),
                unchecked(this::iterm));

        for (Consumer<Integer> page : pages) {
            clearScreen(out);
            page.accept(screenWidth);
            in.read();
        }

        clearScreen(out);

        return 0;
    }

    public String text(String txt) {
        return CommandLine.Help.Ansi.AUTO.text(txt).toString();
    }

    private void ansiColor(int screenWidth) {

        printBox(screenWidth, "ANSI");
        List<String> lines = new ArrayList<>();
        lines.add("                            FG                         REST");
        lines.add("                             |                          | ");
        lines.add("                             v                          v ");
        lines.add(text(
                "System.out.println(\"@|fg(250),bold,italic \\u001B|@@|bold,fg(5) [38;5;5m|@Hello Paris JUG@|bold,italic,fg(250) \\u001B|@@|bold,fg(245) [0m|@\");   // \u001B[38;5;5m Hello Paris JUG\u001B[0m"));
        lines.add("                      ^                           ^ ");
        lines.add("                      |                           | ");
        lines.add("                     ESC                         ESC  ");
        printlnList(lines);

        out.println(text("@|red, Hello|@ @|green World|@"));
        out.println(text("@|bold,bg(4) Hello|@"));

        // format bold,faint,italic,underline,blink,reverse
        out.println(text("""
                @|bold Hello bold|@
                @|faint Hello faint|@
                @|italic Hello italic|@
                @|underline Hello underline|@
                @|blink Hello blink|@
                @|reverse Hello reverse|@
                """));

    }

    private void ansiMove(int screenWidth) throws Exception {

        // jansi
        // Guillaume Nodet
        // org.fusesource.jansi:jansi:2.3.2
        // https://github.com/fusesource/jansi

        System.out.println("");
        System.out.print(ansi().cursorDown(8));
        System.out.println("🦶🦶");
        Thread.sleep(1000);
        System.out.print(ansi().cursorUp(8));
        System.out.println("😆");
        BiFunction<Integer, Integer, String> moveCat = (Integer r, Integer c) -> ansi().cursor(r, c)
                .render("%s:(%s,%s)", "😸", r, c).toString();

        for (int row = 15, column = 20; row > 1 && column < 35; row--, column++) {
            out.println(moveCat.apply(row, column));
            Thread.sleep(300);
        }
    }

    private void spinner(int screenWidth) throws Exception {
        printBox(screenWidth, "Spinner");

        // business Thread
        AtomicReference<String> currentTask = new AtomicReference<>("starting...");
        Future<Boolean> businessLogic = Executors.newSingleThreadExecutor().submit(() -> {
            currentTask.set("init ...");
            waitTime(3000);
            currentTask.set("compile ...");
            waitTime(4000);
            currentTask.set("test ...");
            waitTime(4000);
            currentTask.set("publish artifact...");
            waitTime(4000);

            return true;
        });

        // main thread render the spinner
        final List<String> SPINNER_ICONS = List.of("⡿", "⣟", "⣯", "⣷", "⣾", "⣽", "⣻", "⢿"); // UTF-8 Power !
        int refreshTime = 200;
        for (int i = 0; !businessLogic.isDone(); i++) {
            out.print("\r");
            out.print(ansi().bold().fgBrightYellow().a(SPINNER_ICONS.get(i)).reset());
            out.print(" ");
            out.printf("  %-20s", currentTask.get());
            showCursor(out, i % 2 == 0);
            Thread.sleep(refreshTime);
            if (i == SPINNER_ICONS.size() - 1) {
                i = 0;
            }
        }
    }

    private void progessbar(int screenWidth) throws InterruptedException, IOException {

        // ProgressBar
        // Tongfei Chen
        // https://tongfei.me/progressbar/
        printBox(screenWidth, "me.tongfei:progressbar:0.9.1");

        out.println(text("@|underline mutli progressbar|@"));
        try (ProgressBar pb1 = new ProgressBar("Task 1", 100); ProgressBar pb2 = new ProgressBar("Task 2", 100)) {
            pb1.stepTo(10);
            for (int i = 0; i < 100; i++) {
                if (pb1.getCurrent() < 100)
                    pb1.step();
                pb2.step();

                if (i < 25) {
                    pb1.setExtraMessage("Reading...");
                    pb2.setExtraMessage("Reading...");
                } else if (i < 60) {
                    pb1.setExtraMessage("Process...");
                    pb2.setExtraMessage("Process...");
                } else if (i < 80) {
                    pb1.setExtraMessage("Writing...");
                    pb2.setExtraMessage("Writing...");
                } else {
                    pb1.setExtraMessage("Finishing...");
                    pb2.setExtraMessage("Finishing...");
                }

                Thread.sleep(100);
            }
        }

        hr(2);

        {
            out.println(text("@|underline Wrap a FileInputStream|@"));
            hr(2);
            // Wrap File, Iterator, Stream
            ProgressBarBuilder pbb = new ProgressBarBuilder().setTaskName("Reading " + filePath).setUnit("MB", 1048576)
                    .showSpeed();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ProgressBar.wrap(new FileInputStream(filePath), pbb)))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() % 10 == 0)
                        Thread.sleep(1);
                }
            }
        }
    }

    void tableStringFormat(int screenWidth) throws Exception {

        printBox(screenWidth, "String.format Power !");

        List<String> lines = new ArrayList<>();

        lines.add("+-----------------+------+");
        lines.add("| Column name     | ID   |");
        lines.add("+-----------------+------+");

        Object[][] data = { { "Bird", 16 }, { "Smith", 42 }, { "Grouchant", 8 } };
        for (Object[] datum : data) {
            // align left
            lines.add(String.format("| %-15s | %04d |", datum[0], datum[1]));
        }
        lines.add("+-----------------+------+");
        printlnList(lines);

    }

    void tableFliptables(int screenWidth) {
        // flip-tables
        // *Jake Wharton*
        // 0 dependence
        // https://github.com/JakeWharton/flip-tables
        printBox(screenWidth, "com.jakewharton.fliptables:fliptables:1.1.0");

        String[] headers = { "First Name", "Last Name", "Age", "Type" };
        Object[][] data = { { "Big", "Bird", 16, ansi().bgBrightRed().a("PersonType.COSTUME").reset().toString() },
                { "Joe", "Smith", 42, "PersonType.HUMAN" }, { "Oscar", "Grouchant", 8, "PersonType.PUPPET " } };

        String table = FlipTableConverters.fromObjects(headers, data);
        out.println(table);

    }

    void tableAsciitable(int screenWidth) {
        // asciitable
        // Sven van der Meer
        // "https://github.com/vdmeer/asciitable"
        printBox(screenWidth, "de.vandermeer:asciitable:0.3.2");

        Collection<String> table1;
        Collection<String> table2;
        {
            // Padding
            AsciiTable at = new AsciiTable();
            at.addRule();
            at.addRow("row 1 col 1", "row 1 col 2");
            at.getRawContent().getLast().setTextAlignment(TextAlignment.RIGHT);
            at.addRule();
            at.addRow("row 2 col 1", "row 2 col 2");
            at.getRawContent().getLast().setTextAlignment(TextAlignment.CENTER);
            at.addRule();
            at.addRow("row 3 col 1", "row 3 col 2");
            at.getRawContent().getLast().setTextAlignment(TextAlignment.LEFT);
            at.addRule();
            at.getContext().setGridTheme(TA_GridThemes.LINE_TOPBOTTOM);

            at.setPaddingTopChar('v');
            at.setPaddingBottomChar('^');
            at.setPaddingLeftChar('>');
            at.setPaddingRightChar('<');
            at.setPadding(1);

            table1 = at.renderAsCollection(screenWidth / 3);
        }

        {
            // Span
            AsciiTable at = new AsciiTable();
            at.addRule();
            at.addRow(null, null, null, null, "span all 5 columns");
            at.addRule();
            at.addRow(null, null, null, "span 4 columns", "just 1 column");
            at.addRule();
            at.addRow(null, null, "span 3 columns", null, "span 2 columns");
            at.addRule();
            at.addRow(null, "span 2 columns", null, null, "span 3 columns");
            at.addRule();
            at.addRow("just 1 column", null, null, null, "span 4 columns");
            at.addRule();
            at.addRow("just 1 column", "just 1 column", "just 1 column", "just 1 column", "just 1 column");
            at.addRule();
            at.getContext().setGridTheme(TA_GridThemes.FULL);
            table2 = at.renderAsCollection((screenWidth - screenWidth / 3) - 6);
        }

        // Layout
        Streams.zip(table1.stream(), table2.stream(), (t1, t2) -> String.format(" %s    %s ", t1, t2))
                .forEach(out::println);
    }

    private void graph(int screenWidth) {
        // ASCII-Data
        //
        // https://github.com/MitchTalmadge/ASCII-Data
        printBox(screenWidth, "com.mitchtalmadge:ascii-data:1.4.0");
        out.println(ASCIIGraph.fromSeries(new double[] { 1, 3, 1, 5, 6, 7, 2, 0, 1, 5, 1, 9, 7, 9 }).plot());
    }

    private void jline3(int screenWidth) throws Exception {
        // jline3
        // Guillaume Nodet
        // https://github.com/jline/jline3

        printBox(screenWidth, "org.jline:jline:3.17.1");

        Terminal terminal = TerminalBuilder.builder().dumb(true).build();

        // format border
        int w = terminal.getWidth();
        int h = terminal.getHeight() - 5;
        String topStr = "┌" + "─".repeat(w - 2) + ("┐");
        String wallStr = "│" + " ".repeat(w - 2) + "│";
        String bottomStr = "└" + "─".repeat(w - 2) + "┘";

        System.out.print(topStr);
        for (int i = 0; i < h - 2; i++) {
            System.out.print(wallStr);
        }
        System.out.print(bottomStr);

        String sreanSize = terminal.getSize().toString();
        String terminfoText = "terminfo and tput";

        // use org.jline.utils.WCWidth.wcwidth to handle all UTF-8 char
        Function<String, Integer> center = (String msg) -> w / 2 - msg.length() / 2;

        System.out.print(ansi().cursor(h / 2 - 1, center.apply(terminfoText)).a(terminfoText));
        System.out.print(ansi().cursor(h / 2, center.apply((sreanSize))).a(sreanSize));

        System.out.print(ansi().cursor(terminal.getHeight(), 0));

    }
    // https://iterm2.com/documentation-escape-codes.html
    private void iterm(int screenWidth) throws IOException {
        printBox(screenWidth, " ITerm2 ");

        // we must reset the display
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            out.print(ITerm2Utils.background("000000"));
            out.print(ITerm2Utils.foreground("FFFFFF"));
            showCursor(out, true);
        }));

        try {

            out.print(ITerm2Utils.background("BEDBBB"));
            out.print(ITerm2Utils.foreground("707070"));
            out.println("background: #BEDBBB and foreground: #707070");
            in.read();

        } finally {
            out.print(ITerm2Utils.background("000000"));
            out.print(ITerm2Utils.foreground("FFFFFF"));
        }

        out.println("link :" + ITerm2Utils.link("https://www.canalplus.com", "MyCanal"));
        in.read();

        // ITerm wrap File capability
        // imgcat https://www.iterm2.com/documentation-images.html
        //
        out.println(text("@|underline Images:|@"));
        out.println();
        for (int i = 0; i < 5; i++) {

            out.print(ITerm2Utils.file(Files.readAllBytes(Path.of("mycanal.png")), false, Optional.empty(),
                    Optional.empty(), Optional.empty()));
	    out.print(ansi().cursorUp(4));
        }
        out.println();

    }

    public void printBox(int screenWidth, String ansi) {
        printlnList(center(box(ansi), screenWidth));
    }

    public void printlnList(Collection<String> lines) {
        lines.forEach(out::println);

    }

    public int getScreenWidth() throws IOException {
        var terminal = TerminalBuilder.builder().dumb(true).build();
        terminal.close();
        return terminal.getWidth();
    }

    public void waitTime(int time) {
        try {
            Thread.sleep(time);
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }
}
