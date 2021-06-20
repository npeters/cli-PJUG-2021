///usr/bin/env jbang "$0" "$@" ; exit $?
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:1.11.0.Final}@pom
//DEPS io.quarkus:quarkus-picocli info.picocli:picocli-codegen:4.6.1
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN
//Q:CONFIG quarkus.picocli.native-image.processing.enable=true
//JAVA 11

import picocli.CommandLine;
import picocli.CommandLine.Model.OptionSpec;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.Quarkus;

import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import picocli.CommandLine.*;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import picocli.codegen.docgen.manpage.ManPageGenerator;
import picocli.CommandLine.Help.Ansi;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, name = "HelloWordQuarkus.java", subcommands = {
        HelloCommand.class, GoodByeCommand.class, picocli.AutoComplete.GenerateCompletion.class, ManPageGenerator.class,
        CommandLine.HelpCommand.class }, description = "@|bold Quarkus|@ /@|bold Picocli|@ HelloWord CLI"

)
public class HelloWordQuarkus {
}

enum Language {
    FR, EN
}

@CommandLine.Command(name = "hello", description = "Greet World!")
class HelloCommand implements Runnable {

    @Inject
    SayService sayService;

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Who will we greet? default ${DEFAULT-VALUE}", defaultValue = "World")
    String name;

    @CommandLine.Option(names = { "-l",
            "--language" }, description = "language:${COMPLETION-CANDIDATES} default ${DEFAULT-VALUE} ", defaultValue = "EN")
    Language language;

    @Override
    public void run() {
        System.out.println(sayService.sayHello(name));
    }
}

@CommandLine.Command(name = "goodbye", description = "Say goodbye to World !")
class GoodByeCommand implements Runnable {

    @Inject
    SayService sayService;

    @CommandLine.Option(names = { "-n", "--name" }, description = "Who will we greet?", defaultValue = "World")
    String name;

    @CommandLine.Option(names = { "-l", "--language" }, description = "language", defaultValue = "EN")
    Language language;

    @Override
    public void run() {
        System.out.println(sayService.sayGoodbye(name));
    }
}

interface SayService {
    String sayHello(String name);

    String sayGoodbye(String name);
}

class FrenshSayService implements SayService {
    public String sayHello(String name) {
        return "Bonjour " + name + "!";
    }

    public String sayGoodbye(String name) {
        return "Au revoir  " + name + "!";
    }
}

class EnglishSayService implements SayService {
    public String sayHello(String name) {
        return "Hello " + name + "!";
    }

    public String sayGoodbye(String name) {
        return "Goodbye " + name + "!";
    }
}

@ApplicationScoped
class ServiceConfiguration {

    /**
     * Picocli extra configuration
     * 
     * @param factory
     * @return
     */
    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) {
        return factory.create().setCaseInsensitiveEnumValuesAllowed(true);
    }

    @Produces
    @ApplicationScoped
    SayService buildSayService(CommandLine.ParseResult parseResult) {

        Language lang;
        // use parseResult to customize the injection of SayService
        OptionSpec languageOptionSpec = parseResult.subcommand().matchedOption('l');
        if (languageOptionSpec == null) {
            lang = Language.EN;
        } else {
            lang = languageOptionSpec.getValue();
        }

        SayService service;
        switch (lang) {
            case FR:
                service = new FrenshSayService();
                break;
            case EN:
                service = new EnglishSayService();
                break;
            default:
                service = new EnglishSayService();
                break;
        }
        return service;
    }
}
