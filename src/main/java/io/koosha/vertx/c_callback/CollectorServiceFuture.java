package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CollectorServiceFuture extends AbstractVerticle {

    private final Logger log = LoggerFactory.getLogger(CollectorServiceFuture.class);

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
        CompositeFuture
            .all(
                fetch(Config.SENSOR_PORT_BASE),
                fetch(Config.SENSOR_PORT_BASE + 1),
                fetch(Config.SENSOR_PORT_BASE + 2)
            )
            .flatMap(this::sendToSnapshot)
            .onSuccess(data -> req
                .response()
                .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__JSON)
                .end(data.encode()))
            .onFailure(event -> {
                log.error("failed", event.getCause());
                req.response().setStatusCode(500).end();
            });

    }

    private Future<JsonObject> sendToSnapshot(final CompositeFuture future) {
        final JsonObject data = Util.obj("data", Util.arrOf(
            future.<JsonObject>list().get(0),
            future.<JsonObject>list().get(1),
            future.<JsonObject>list().get(2)
        ));
        return this.webClient.post(Config.SNAPSHOT_PORT, "localhost", "/")
                             .expect(ResponsePredicate.SC_SUCCESS)
                             .sendJson(data)
                             .map(response -> data);
    }

    private Future<JsonObject> fetch(final int port) {
        return this.webClient
            .get(port, "localhost", "/")
            .expect(ResponsePredicate.SC_SUCCESS)
            .as(BodyCodec.jsonObject())
            .send()
            .map(HttpResponse::body);
    }

}
