package io.koosha.vertx.b_stream;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

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
                    .handler(buffer -> handleBuffer(socket, buffer))
                    .endHandler(it -> log.info("connection ended from: {}", socket.remoteAddress()));
    }

    private void handleBuffer(final NetSocket socket,
                              final Buffer buffer) {

        final Handler<Throwable> failHandler = event -> log.warn("failed to write to client at: {}", socket.remoteAddress());

        final String command = buffer.toString(StandardCharsets.UTF_8);
        switch (command) {
            case "/list":
                log.info("executing list");
                vertx.eventBus().request(Config.ENDPOINT__LIST, "", reply -> {
                    if (reply.succeeded())
                        ((JsonObject) reply.result().body())
                            .getJsonArray("files")
                            .stream().forEach(name ->
                            socket.write(name + "\n")
                                  .onFailure(failHandler));
                    else
                        socket.write("error\n")
                              .onFailure(failHandler);
                });
                break;

            case "/play":
                log.info("executing play");
                vertx.eventBus().send(Config.ENDPOINT__PLAY, "");
                socket.write("playing...\n")
                      .onFailure(failHandler);
                break;

            case "/pause":
                log.info("executing pause");
                vertx.eventBus().send(Config.ENDPOINT__PAUSE, "");
                socket.write("paused\n")
                      .onFailure(failHandler);
                break;

            default:
                if (command.startsWith("/schedule ")) {
                    final String track = command.split(" ", 2)[1];
                    log.info("executing schedule={}", track);
                    vertx.eventBus()
                         .send(Config.ENDPOINT__SCHEDULE, Util.obj("file", track));
                    socket.write("scheduled: " + track + "\n")
                          .onFailure(failHandler);
                }
                else {
                    log.info("unknown command={}", command);
                    socket.write("Unknown command: " + command + "\n")
                          .onFailure(failHandler);
                }
        }
    }

}
