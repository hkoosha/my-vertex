package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class CollectorServiceCallbacks extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(CollectorServiceCallbacks.class);

    private WebClient webClient;

    @Override
    public void start(final Promise<Void> startPromise) {
        this.webClient = WebClient.create(vertx);

        vertx.createHttpServer()
             .requestHandler(this::handler)
             .listen(Config.COLLECTOR_PORT)
             .onFailure(event -> {
                 log.error("failed to start collector service server on: {}", Config.COLLECTOR_PORT);
                 startPromise.fail(event.getCause());
             })
             .onSuccess(event -> {
                 log.error("started collector service server on: {}", Config.COLLECTOR_PORT);
                 startPromise.complete();
             });
    }

    private void handler(final HttpServerRequest req) {
        final List<JsonObject> responses = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            webClient.get(Config.SENSOR_PORT_BASE + i, "localhost", "/")
                     .expect(ResponsePredicate.SC_SUCCESS)
                     .as(BodyCodec.jsonObject())
                     .send(ar -> {
                         if (ar.succeeded())
                             responses.add(ar.result().body());
                         else
                             log.error("could not reach sensor", ar.cause());
                         if (counter.incrementAndGet() == 3)
                             sendToSnapshot(req, Util.obj(Config.FIELD__DATA, Util.arr(responses)));
                     });
        }
    }

    private void sendToSnapshot(final HttpServerRequest req,
                                final JsonObject data) {
        webClient.post(Config.SNAPSHOT_PORT, "localhost", "/")
                 .expect(ResponsePredicate.SC_SUCCESS)
                 .sendJsonObject(data, ar -> {
                     if (ar.succeeded()) {
                         sendResponse(req, data);
                     }
                     else {
                         log.error("could not reach snapshot server", ar.cause());
                         Util.err500(req);
                     }
                 });
    }

    private void sendResponse(final HttpServerRequest req,
                              final JsonObject data) {
        req.response()
           .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__JSON)
           .end(data.encode());
    }
}
