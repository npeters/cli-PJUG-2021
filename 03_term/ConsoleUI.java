///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1
//DEPS org.fusesource.jansi:jansi:2.3.2
//DEPS org.jline:jline:3.17.1

//DEPS de.codeshelf.consoleui:consoleui:0.0.13
//SOURCES utils/JAnsiUtils.java utils/ITerm2Utils.java
//JAVA 16

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Map;

import java.util.concurrent.Callable;

import static java.lang.System.*;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.prompt.ConfirmResult;
import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;

import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import org.fusesource.jansi.AnsiConsole;
import utils.ITerm2Utils;
import utils.JAnsiUtils;

import java.io.Console;
import java.io.File;

import java.util.HashMap;

import static org.fusesource.jansi.Ansi.ansi;
import static utils.JAnsiUtils.*;

@Command(name = "ConsoleUI", mixinStandardHelpOptions = true, version = "ConsoleUI 0.1", description = "ConsoleUI made with jbang")
class ConsoleUI implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new ConsoleUI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {

        try {

            out.println(ITerm2Utils.clearScrollbackHistory());

            // Secure password
            Console cons;
            char[] passwd;

            out.println(text("@|underline Secure password |@"));

            if ((cons = System.console()) != null && (passwd = cons.readPassword("[%s]", "Password:")) != null) {
                out.println("Print password: " + new String(passwd));
                java.util.Arrays.fill(passwd, ' ');
            }
            in.read();
            clearScreen(out);

            AnsiConsole.systemInstall();

            // Andreas Wegmann
            // https://github.com/awegmann/consoleui
            printBox(120,"de.codeshelf.consoleui:consoleui:0.0.13");

            System.out.println(ansi().render("""
                    @|red,italic Hello|@ @|green World|@
                    @|reset  This is a demonstration of ConsoleUI java library. It provides a simple console interface
                    for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written
                    in JavaScript.|@"""));

            ConsolePrompt prompt = new ConsolePrompt();
            PromptBuilder promptBuilder = prompt.getPromptBuilder();

            promptBuilder.createInputPrompt().name("name").message("Please enter your name").defaultValue("John Doe")
                    .addCompleter(new StringsCompleter("Jim", "Jack", "John")).addPrompt();

            promptBuilder.createListPrompt().name("pizzatype").message("Which pizza do you want?").newItem()
                    .text("Margherita").add() // without name (name defaults to text)
                    .newItem("veneziana").text("Veneziana").add().newItem("hawai").text("Hawai").add()
                    .newItem("quattro").text("Quattro Stagioni").add().addPrompt();

            promptBuilder.createCheckboxPrompt().name("topping").message("Please select additional toppings:")

                    .newSeparator("standard toppings").add()

                    .newItem().name("cheese").text("Cheese").add().newItem("bacon").text("Bacon").add()
                    .newItem("onions").text("Onions").disabledText("Sorry. Out of stock.").add()

                    .newSeparator().text("special toppings").add()

                    .newItem("salami").text("Very hot salami").check().add().newItem("salmon").text("Smoked Salmon")
                    .add()

                    .newSeparator("and our speciality...").add()

                    .newItem("special").text("Anchovies, and olives").checked(true).add().addPrompt();

            promptBuilder.createChoicePrompt().name("payment").message("How do you want to pay?")

                    .newItem().name("cash").message("Cash").key('c').asDefault().add().newItem("visa")
                    .message("Visa Card").key('v').add().newItem("master").message("Master Card").key('m').add()
                    .newSeparator("online payment").add().newItem("paypal").message("Paypal").key('p').add()
                    .addPrompt();

            promptBuilder.createConfirmPromp().name("delivery").message("Is this pizza for delivery?")
                    .defaultValue(ConfirmChoice.ConfirmationValue.YES).addPrompt();

            HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
            System.out.println(
                    "result = \n" + String.join("\n", result.entrySet().stream().map(Map.Entry::toString).toList()));

            System.out.println();
            ConfirmResult delivery = (ConfirmResult) result.get("delivery");
            if (delivery.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                System.out.println("We will deliver the pizza in 5 minutes");
            }
            return 0;

        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
