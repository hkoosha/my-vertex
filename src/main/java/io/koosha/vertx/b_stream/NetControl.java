package io.koosha.vertx.b_stream;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class NetControl extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(NetControl.class);

    @Override
    public void start() {
        vertx.createNetServer()
             .connectHandler(this::handler)
             .listen(Config.NET_CONTROL_PORT)
             .onFailure(event -> log.error("failed to start NetControl", event.getCause()))
             .onSuccess(event -> log.info("NetControl started on {}", Config.NET_CONTROL_PORT));
    }

    private void handler(final NetSocket socket) {
        log.info("new connection from: {}", socket.remoteAddress());
        RecordParser.newDelimited("\n", socket)
                    .handler(buffer -> command(socket, buffer.toString(StandardCharsets.UTF_8)))
                    .endHandler(it -> log.info("connection ended from: {}", socket.remoteAddress()));
    }

    private void command(final NetSocket socket,
                         final String command) {
        switch (command) {
            case "list":
            case "/list":
                log.info("executing list");
                vertx.eventBus().<JsonObject>request(Config.ENDPOINT__LIST, null, reply ->
                    say(reply.failed()
                        ? "error: " + reply.cause().getMessage()
                        : reply.result()
                               .body()
                               .getJsonArray(Config.PARAM__FILES)
                               .stream()
                               .map(Object::toString)
                               .collect(Collectors.joining("\n")), socket));
                break;

            case "play":
            case "/play":
                log.info("executing play");
                vertx.eventBus().send(Config.ENDPOINT__PLAY, null);
                say("playing...", socket);
                break;

            case "pause":
            case "/pause":
                log.info("executing pause");
                vertx.eventBus().send(Config.ENDPOINT__PAUSE, null);
                say("paused", socket);
                break;

            default:
                if (command.startsWith("/schedule ") || command.startsWith("schedule ")) {
                    final String track = command.split(" ", 2)[1];
                    log.info("executing schedule={}", track);
                    vertx.eventBus()
                         .send(Config.ENDPOINT__SCHEDULE, Util.obj(Config.PARAM__FILE_NAME, track));
                    say("scheduled: " + track, socket);
                }
                else {
                    log.info("unknown command={}", command);
                    say("unknown command: " + command, socket);
                }
        }
    }

    private static void say(final String msg,
                            final NetSocket socket) {
        socket.write(msg + "\n").onFailure(event ->
            log.warn("failed to write to client at: {}", socket.remoteAddress()));
    }

}
