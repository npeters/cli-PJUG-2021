import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pivovarit.function.ThrowingSupplier;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Command(
    name = "jwt-cli",
    version = "Version 0.0.2",
    usageHelpWidth = 60,
    header =
            "   _____                  _         \n" +
            "  /  __ \\                | |  _    \n" +
            "  | /  \\/ __ _ _ __   __ | |_| |_  \n" +
            "  | |    / _` | '_ \\ / _`| |_   _| \n" +
            "  | \\__/\\ (_| | | | | (_|| | |_|  \n" +
            "  \\____/\\__,_|_| |_|\\__,_|_|     \n",

    footer = "Provided by Canal+",
    description = "Custom @|bold,underline styles|@ and @|fg(red) colors|@."

)
public class JwtCli implements Callable<Void> {

  public static void main(String[] args) {

    CommandLine cmd = new CommandLine(new JwtCli())
        .addSubcommand("encode", new Encode(System.out))
        .addSubcommand("decode", new Decode(System.out, wrapperStdin()))
        .addSubcommand("help", new HelpCommand());


    cmd.parseWithHandler(new RunLast(), args);
  }

  @Spec
  Model.CommandSpec spec;

  @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
  boolean versionInfoRequested;

  @Option(names = {"--autocomplete"})
  boolean autocomplete = false;

  @Override
  public Void call() throws Exception {
    if (autocomplete) {
      System.out.println(picocli.AutoComplete.bash("jwt-cli", spec.commandLine()));
    } else {
      CommandLine.usage(this.spec, System.out);
    }
    return null;
  }

  private static class CommonOption {
    @CommandLine.Option(names = {"-s", "--secret"}, description = "Passphrase", defaultValue = "secret")
    private String secret;

  }

  @Command(name = "encode", helpCommand = true, sortOptions = false,description = "encode jwt token")
  public static class Encode implements Callable<Void> {
    private static final int DEFAULT_EXPIRES_AT = 6; // 6 Month

    @Mixin
    private CommonOption commonOption;

    @Option(names = {"-i", "--issuer"}, required = true, order = 1, description = "jwt issuer")
    private String issuer;

    @Option(names = {"-e", "--expiresAt"}, order = 2,description = "expiration time (YYYY-MM-DD)")
    private LocalDate expiresAt;

    @Option(names = {"-c", "--claim"}, required = true, arity = "1..*", order = 3,description = "add a claim(s)")
    private Map<String, String> claims;

    PrintStream out;

    public Encode() {

    }

    public Encode(PrintStream out) {
      this.out = out;
    }


    @Override
    public Void call() throws Exception {
      LocalDate expire = expiresAt == null ? LocalDate.now().withMonth(DEFAULT_EXPIRES_AT) : expiresAt;
      Algorithm algorithm = Algorithm.HMAC256(commonOption.secret);

      JWTCreator.Builder builder = JWT.create()
          .withIssuer(this.issuer)
          .withExpiresAt(Date.from(expire.atStartOfDay(ZoneId.systemDefault()).toInstant()));

      claims.forEach(builder::withClaim);
      String token = builder.sign(algorithm);

      out.println(token);
      return null;
    }
  }

  public enum OutputFormat {
    json,
    text,
    table

  }

  @Command(name = "decode", helpCommand = true,description = "decode jwt token")
  public static class Decode implements Callable<Void> {

    @Mixin
    private CommonOption commonOption;

    @Option(names = {"-o", "--output"}, defaultValue = "table", description = "output format: table,text,json")
    private OutputFormat outputFormat;

    @Parameters(description = "token")
    private String token;

    PrintStream out;
    Supplier<String> in;

    public Decode(PrintStream out, Supplier<String> in) {
      this.out = out;
      this.in = in;
    }

    public Decode() {

    }

    @Override
    public Void call() throws Exception {
      Algorithm algorithm = Algorithm.HMAC256(commonOption.secret);
      JWTVerifier verifier = JWT.require(algorithm).build();

      String tokenAsStr = token;
      if ("-".equals(token)) {
        tokenAsStr = in.get();
      }

      DecodedJWT jwt = verifier.verify(tokenAsStr);

      String result = null;
      switch (outputFormat) {
        case json: {
          result = formatJson(toMap(jwt));
          break;
        }
        case text: {
          result = formatText(toMap(jwt));
          break;
        }
        case table: {
          result = formatTable(toMap(jwt));
          break;
        }
      }

      out.println(result);
      return null;
    }


    private Map<String, String> toMap(DecodedJWT jwt) {
      return jwt.getClaims().entrySet().stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  e -> {
                    if (e.getValue().asString() == null) {
                      return DateTimeFormatter.ISO_INSTANT.format(e.getValue().asDate().toInstant());
                    } else {
                      return e.getValue().asString();
                    }
                  }
              )
          );
    }

    private String formatJson(Map<String, String> allClaims) throws JsonProcessingException {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(allClaims);
    }

    private String formatText(Map<String, String> allClaims) {
      final StringBuilder buf = new StringBuilder();
      allClaims.forEach(
          (String name, String value) -> buf.append(name).append(":").append(value).append("\n")
      );
      return buf.toString();
    }

    private String formatTable(Map<String, String> allClaims) {
      Optional<Integer> nameColumnMax = allClaims
          .keySet()
          .stream()
          .map(String::length)
          .max(Integer::compareTo);

      Optional<Integer> valueMax = allClaims
          .values()
          .stream()
          .map(String::length)
          .max(Integer::compareTo);


      CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumnWidths(CommandLine.Help.Ansi.AUTO, nameColumnMax.orElse(30) + 20, valueMax.orElse(30));

      textTable.addRowValues("Name", "Value");
      textTable.addEmptyRow();
      allClaims.forEach(
          (String name, String value) -> textTable.addRowValues(name, value)
      );

      return textTable.toString();
    }
  }

  private static Supplier<String> wrapperStdin() {
   return  ThrowingSupplier.unchecked(() -> new String(System.in.readAllBytes(), StandardCharsets.UTF_8));
  }

  private String formatJson(Map<String, String> allClaims) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(allClaims);
  }


  private Map<String, String> toMap(DecodedJWT jwt) {
    return jwt.getClaims().entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                  if (e.getValue().asString() == null) {
                    return DateTimeFormatter.ISO_INSTANT.format(e.getValue().asDate().toInstant());
                  } else {
                    return e.getValue().asString();
                  }
                }
            )
        );
  }

  private String formatText(Map<String, String> allClaims) {
    final StringBuilder buf = new StringBuilder();
    allClaims.forEach(
        (String name, String value) -> buf.append(name).append(":").append(value).append("\n")
    );
    return buf.toString();
  }

  private String formatTable(Map<String, String> allClaims) {
    Optional<Integer> nameColumnMax = allClaims
        .keySet()
        .stream()
        .map(String::length)
        .max(Integer::compareTo);

    Optional<Integer> valueMax = allClaims
        .values()
        .stream()
        .map(String::length)
        .max(Integer::compareTo);


    Help.TextTable textTable = Help.TextTable.forColumnWidths(Help.Ansi.AUTO, nameColumnMax.orElse(30) + 20, valueMax.orElse(30));

    textTable.addRowValues("Name", "Value");
    textTable.addEmptyRow();
    allClaims.forEach(
        (String name, String value) -> textTable.addRowValues(name, value)
    );

    return textTable.toString();
  }


}




