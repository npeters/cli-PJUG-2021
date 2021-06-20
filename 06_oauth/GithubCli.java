///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1 info.picocli:picocli-codegen:4.6.1 com.fasterxml.jackson.core:jackson-databind:2.12.3
//DEPS com.pivovarit:throwing-function:1.5.1
//JAVA 16

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static java.lang.System.out;
import static com.pivovarit.function.ThrowingConsumer.unchecked;

@Command(name = "GithubCli", mixinStandardHelpOptions = true, version = "GithubCli 0.1", description = "GithubCli made with jbang")
class GithubCli implements Callable<Integer> {

    // https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps#rate-limits-for-the-device-flow
    record DeviceCodeResponse(String device_code, String user_code, String verification_uri, long expires_in,
            int interval) {
    }

    interface AccessTokenResponse {
    }

    record SuccessAccessTokenResponse(String access_token, String token_type, String scope)
            implements AccessTokenResponse {
    }

    record ErrorAccessTokenResponse(String error, String error_description, String error_uri)
            implements AccessTokenResponse {
    }

    // App ID
    String CLIENT_ID = "3084f75f3fb615b86b22";

    @Parameters(index = "0", description = "The greeting to print", defaultValue = "World!")
    private String greeting;

    public static void main(String... args) {
        int exitCode = new CommandLine(new GithubCli()).execute(args);
        System.exit(exitCode);
    }

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        HttpClient client = HttpClient.newHttpClient();

        DeviceCodeResponse deviceCodeResponse = getDeviceCodeResponse(client);

        out.println("User code: " + deviceCodeResponse.user_code);
        Runtime.getRuntime().exec(new String[] { "open", deviceCodeResponse.verification_uri });

        long expiresInstant = ZonedDateTime.now().plusSeconds(deviceCodeResponse.expires_in).toInstant().toEpochMilli();
        boolean hasSuccess = false;
        AccessTokenResponse accessTokenResponse = null;
        while (expiresInstant > System.currentTimeMillis() && !hasSuccess) {

            accessTokenResponse = getAccessTokenResponse(client, deviceCodeResponse);

            if (accessTokenResponse instanceof ErrorAccessTokenResponse error) {
                out.printf("%s %s %n", error.error, error.error_description);
                out.printf("wait...%n");
                Thread.sleep(deviceCodeResponse.interval * 1000L);
            } else if (accessTokenResponse instanceof SuccessAccessTokenResponse success) {
                hasSuccess = true;
            }
        }

        if (accessTokenResponse instanceof ErrorAccessTokenResponse error) {
            out.printf("%s %s %n", error.error, error.error_description);
            return -1;
        } else if (accessTokenResponse instanceof SuccessAccessTokenResponse success) {
            out.printf("access_token: %s %n", success.access_token);
        }

        return 0;
    }

    private AccessTokenResponse getAccessTokenResponse(HttpClient client, DeviceCodeResponse deviceCodeResponse)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        AccessTokenResponse accessTokenResponse;
        var accessTokenRequest = HttpRequest.newBuilder().uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(String.format(
                        "client_id=%s&device_code=%s&grant_type=urn:ietf:params:oauth:grant-type:device_code",
                        CLIENT_ID, deviceCodeResponse.device_code)))
                .build();

        accessTokenResponse = client.sendAsync(accessTokenRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body).thenApply((String s) -> {
                    try {
                        // out.println(s);
                        var jsonNode = mapper.readTree(s);
                        if (jsonNode.has("error")) {
                            return mapper.treeToValue(jsonNode, ErrorAccessTokenResponse.class);
                        } else {
                            return mapper.treeToValue(jsonNode, SuccessAccessTokenResponse.class);
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();
        return accessTokenResponse;
    }

    private DeviceCodeResponse getDeviceCodeResponse(HttpClient client)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        var decivceCodeRequest = HttpRequest.newBuilder().uri(URI.create("https://github.com/login/device/code"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("client_id=" + CLIENT_ID)).build();

        var deviceCodeResponse = client.sendAsync(decivceCodeRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body).thenApply((String s) -> {
                    try {
                        return mapper.readValue(s, DeviceCodeResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();
        return deviceCodeResponse;
    }
}
