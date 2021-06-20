///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS io.vertx:vertx-http-proxy:4.1.0 io.vertx:vertx-web:4.1.0

//JAVA 16

import io.vertx.core.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(name = "webserver", mixinStandardHelpOptions = true, version = "webserver 0.1", description = "webserver made with jbang")
class webserver extends AbstractVerticle implements Callable<Integer> {

    @Parameters(index = "0", description = "The greeting to print", defaultValue = "World!")
    private String greeting;

    public static void main(String... args) {
        int exitCode = new CommandLine(new webserver()).execute(args);
        System.exit(exitCode);
    }


    @Override
    public void start() throws Exception {

        vertx.createHttpServer().requestHandler(request -> {
            String file = "";
            if (request.path().equals("/")) {
                file = "index.html";
            } else if (!request.path().contains("..")) {
                file = request.path();
            }
            request.response().sendFile("." + file);
        }).listen(8099).onSuccess(server -> System.out.println("HTTP server started on port " + server.actualPort()));

    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        Vertx vertx = Vertx.vertx();
        var f = vertx.deployVerticle(this,new DeploymentOptions());


        var t = new Thread(()-> {
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
        t.join();
        return 0;
    }
}
