package io.koosha.vertx.heat;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.TimeoutStream;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class HttpServer extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    private static final String RESOURCE = "index.html";

    private String indexHtml;

    public void start(final Promise<Void> startPromise) throws IOException {
        this.indexHtml = Util.readResource(RESOURCE);

        this.vertx.createHttpServer()
                  .requestHandler(this::handler)
                  .listen(8080)
                  .onSuccess(event -> {
                      log.info("started server on :8080");
                      startPromise.complete();
                  })
                  .onFailure(cause -> {
                      log.info("server start failed", cause);
                      startPromise.fail(cause);
                  });
    }

    private void handler(final HttpServerRequest req) {
        switch (req.path()) {
            case "/":
                req.response().end(this.indexHtml);
                break;

            case "/sse":
                this.sse(req);
                break;

            default:
                req.response().setStatusCode(404).end();
        }
    }

    private void sse(final HttpServerRequest req) {
        req.response()
           .putHeader("Content-Type", "text/event-stream")
           .putHeader("Cache-Control", "no-cache")
           .setChunked(true);

        final MessageConsumer<JsonObject> consumer = this.vertx
            .eventBus()
            .consumer(Config.ENDPOINT__SENSOR_UPDATES);
        consumer.handler(event -> req
            .response()
            .write("event: update\ndata: " + event.body().encode() + "\n\n")
            .onFailure(failure -> log.warn("failed to write to client at: {}", req.remoteAddress())));

        final TimeoutStream ticks = this.vertx.periodicStream(1000L);
        ticks.handler(event -> this.vertx
            .eventBus()
            .request(Config.ENDPOINT__SENSOR_AVERAGE, "", reply -> {
                if (reply.succeeded()) {
                    req.response().write("event: update\n");
                    req.response().write("data: " + ((JsonObject) reply.result().body()).encode() + "\n\n");
                }
            }));

        req.response().endHandler(event -> {
            log.info("client leaving: {}", req.remoteAddress());
            try {
                consumer.unregister();
            }
            finally {
                ticks.cancel();
            }
        });
    }

}
