import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class JLineDemo {

  public static void main(String[] args) {
    try {
      String prompt = "prompt> ";
      String rightPrompt = null;

      Terminal terminal = TerminalBuilder.builder().build();

      PicocliJLineCompleter completer = new PicocliJLineCompleter();
      LineReader reader = LineReaderBuilder.builder().terminal(terminal).completer(completer)
          .parser(new DefaultParser()).build();

      CliCommands commands = new CliCommands();
      commands.setReader(reader);

      // load command
      CommandLine cmd = new CommandLine(commands);

      try (PrintStream out = new PrintStream(reader.getTerminal().output())) {
        cmd.addSubcommand("cls", new ClearScreen())
           .addSubcommand("cmd", new MyCommand())
           .addSubcommand("decode", new JwtCli.Decode(out, () -> ""))
           .addSubcommand("encode", new JwtCli.Encode(out));
        cmd.usage(out);
        completer.setSpec(cmd.getCommandSpec());

        // read line loop
        // read command and run it
        String line;
        while (true) {
          try {
            line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
            ParsedLine pl = reader.getParser().parse(line, 0);
            String[] arguments = pl.words().toArray(new String[0]);
            cmd.parseWithHandler(new CommandLine.RunLast(), arguments);

          } catch (UserInterruptException e) {
            // Ignore
          } catch (EndOfFileException e) {
            return;
          }
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  @CommandLine.Command(name = "", description = "Example interactive shell with completion", footer = { "",
      "Press Ctl-D to exit." })
  static class CliCommands implements Runnable {
    LineReaderImpl reader;
    PrintWriter out;

    CliCommands() {
    }

    public void setReader(LineReader reader) {
      this.reader = (LineReaderImpl) reader;
      out = reader.getTerminal().writer();
    }

    public void run() {

      System.err.println("CliCommands.run");
      out.println(new CommandLine(this).getUsageMessage());
    }
  }

  /**
   * A command with some options to demonstrate completion.
   **/
  @CommandLine.Command(name = "cmd", mixinStandardHelpOptions = true, version = "1.0", description = "Command with some options to demonstrate TAB-completion"
      + " (note that enum values also get completed)")
  static class MyCommand implements Runnable {
    @CommandLine.Option(names = { "-v", "--verbose" })
    private boolean[] verbosity = {};

    @CommandLine.Option(names = { "-d", "--duration" })
    private int amount;

    @CommandLine.Option(names = { "-u", "--timeUnit" })
    private TimeUnit unit;

    @CommandLine.ParentCommand
    CliCommands parent;

    public void run() {
      if (verbosity.length > 0) {
        parent.out.printf("Hi there. You asked for %d %s.%n", amount, unit);
      } else {
        parent.out.println("hi!");
      }
    }
  }

  /**
   * Command that clears the screen.
   **/
  @CommandLine.Command(aliases = "clear", mixinStandardHelpOptions = true, description = "Clears the screen", version = "1.0")
  static class ClearScreen implements Callable<Void> {

    @CommandLine.ParentCommand
    CliCommands parent;

    public Void call() throws IOException {
      parent.reader.clearScreen();
      return null;
    }
  }

}
