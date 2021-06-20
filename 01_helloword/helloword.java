///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0 info.picocli:picocli-codegen:4.6.1

//DEPS com.github.lalyos:jfiglet:0.0.8
//FILES Doom.flf

//JAVA 16

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import static java.lang.System.*;
import java.util.concurrent.Callable;

@Command(name = "helloword", mixinStandardHelpOptions = true, version = "helloword 0.1", description = "helloword made with jbang")
class helloword implements Callable<Integer> {

    @Parameters(index = "0", description = "The greeting to print", defaultValue = "World!")
    private String greeting;

    public static void main(String... args) {
        int exitCode = new CommandLine(new helloword()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        java.lang.System.out.println(
                com.github.lalyos.jfiglet.FigletFont.convertOneLine(new java.io.File("./Doom.flf"), "Hello PJUG "));
        return 0;
    }
}
