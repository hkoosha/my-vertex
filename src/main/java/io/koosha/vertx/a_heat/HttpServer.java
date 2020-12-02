package io.koosha.vertx.a_heat;

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

        final int port = config().getInteger(Config.CFG__PORT, Config.DEFAULT_PORT);

        this.vertx.createHttpServer()
                  .requestHandler(this::handler)
                  .listen(port)
                  .onSuccess(event -> {
                      log.info("started server on :{}", port);
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
                Util.err404(req);
        }
    }

    private void sse(final HttpServerRequest req) {
        req.response()
           .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__EVENT_STREAM)
           .putHeader(Util.HEADER__CACHE_CONTROL, Util.HEADER__CACHE_CONTROL__NO_CACHE)
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
                if (reply.succeeded())
                    req.response().write("event: update\ndata: " + ((JsonObject) reply.result().body()).encode() + "\n\n")
                       .onFailure(failure -> log.warn("failed to write event to client at {}", req.remoteAddress()));
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
