package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class HeatSensor extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HeatSensor.class);

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final String id = UUID.randomUUID().toString();

    private double temperature = 21.0D;

    public void start() {
        final Integer port = config().getInteger(Config.CFG__PORT);
        if (port == null)
            throw new IllegalStateException("port not specified");

        vertx.createHttpServer()
             .requestHandler(this::handle)
             .listen(port)
             .onSuccess(event -> log.info("heat sensor server started on: {}", port))
             .onFailure(event -> log.error("failed to start heat sensor server on {}", port, event.getCause()));

        this.scheduleNextUpdate();
    }

    private void handle(final HttpServerRequest req) {
        final JsonObject data = Util.obj(Config.FIELD__ID, this.id)
                                    .put(Config.FIELD__TEMPERATURE, this.temperature);

        req.response()
           .putHeader(Util.HEADER__CONTENT_TYPE, Util.HEADER__CONTENT_TYPE__JSON)
           .end(data.encode());
    }

    private void scheduleNextUpdate() {
        this.vertx.setTimer(this.random.nextInt(5000) + 1000, this::update);
    }

    private void update(long timerId) {
        final double delta = this.random.nextInt() > 0
            ? this.random.nextGaussian()
            : -this.random.nextGaussian();
        this.temperature += delta / 10.0D;
        this.scheduleNextUpdate();
    }

}
