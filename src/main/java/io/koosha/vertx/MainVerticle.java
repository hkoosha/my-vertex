package io.koosha.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(final Promise<Void> startPromise) {
        final HttpRequest<String> req = WebClient
            .create(this.vertx)
            .get(443, "wikipedia.com", "/")
            .ssl(true)
            .as(BodyCodec.string())
            .expect(ResponsePredicate.SC_OK);
        req.send(event -> {
            if (event.succeeded())
                log.info("got: {}", event.result().body());
            else
                startPromise.fail(event.cause());
            vertx.close();
        });
    }

    public static void main(String... args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }

}
