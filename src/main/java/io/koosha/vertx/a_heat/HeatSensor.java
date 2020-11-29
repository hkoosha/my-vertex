package io.koosha.vertx.a_heat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class HeatSensor extends AbstractVerticle {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final String id = UUID.randomUUID().toString();

    private double temperature = 21.0D;

    public void start() {
        this.scheduleNextUpdate();
    }

    private void scheduleNextUpdate() {
        this.vertx.setTimer(this.random.nextInt(5000) + 1000, this::update);
    }

    private void update(long timerId) {
        final double delta = this.random.nextInt() > 0
            ? this.random.nextGaussian()
            : -this.random.nextGaussian();
        this.temperature += delta / 10.0D;

        final JsonObject payload = new JsonObject()
            .put(Config.FIELD__ID, this.id)
            .put(Config.FIELD__TEMPERATURE, this.temperature);
        this.vertx.eventBus().publish(Config.ENDPOINT__SENSOR_UPDATES, payload);

        this.scheduleNextUpdate();
    }

}
