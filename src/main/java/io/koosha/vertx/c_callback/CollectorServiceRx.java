package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CollectorServiceRx extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(CollectorServiceRx.class);

    private WebClient webClient;

    @Override
    public Completable rxStart() {
        this.webClient = WebClient.create(vertx);

        return vertx.createHttpServer()
                    .requestHandler(this::handler)
                    .rxListen(Config.COLLECTOR_PORT)
                    .ignoreElement();
    }

    private void handler(final HttpServerRequest req) {
        final Single<JsonObject> data = this.collectTemperature();
        this.sendToSnapshot(data).subscribe(
            json -> req.response()
                       .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__JSON)
                       .end(json.encode()),
            err -> {
                log.error("error", err);
                req.response().setStatusCode(500).end();
            });
    }

    private Single<JsonObject> collectTemperature() {
        final Single<HttpResponse<JsonObject>> r0 = fetchTemperature(Config.SENSOR_PORT_BASE);
        final Single<HttpResponse<JsonObject>> r1 = fetchTemperature(Config.SENSOR_PORT_BASE + 1);
        final Single<HttpResponse<JsonObject>> r2 = fetchTemperature(Config.SENSOR_PORT_BASE + 2);
        return Single.zip(r0, r1, r2, (j0, j1, j2) -> Util.obj("data",
            Util.arrOf(j0.body(), j1.body(), j2.body())));
    }

    private Single<HttpResponse<JsonObject>> fetchTemperature(final int port) {
        return this.webClient
            .get(port, "localhost", "")
            .expect(ResponsePredicate.SC_SUCCESS)
            .as(BodyCodec.jsonObject())
            .rxSend();
    }

    private Single<JsonObject> sendToSnapshot(final Single<JsonObject> data) {
        return data.flatMap(json -> this.webClient
            .post(Config.SNAPSHOT_PORT, "localhost", "")
            .expect(ResponsePredicate.SC_SUCCESS)
            .rxSendJsonObject(json)
            .flatMap(resp -> Single.just(json)));
    }

}
