package io.koosha.vertx.a_heat;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class SensorData extends AbstractVerticle {

    private final Map<String, Double> sensorLastValue = new HashMap<>();

    public void start() {
        this.vertx.eventBus().consumer(Config.ENDPOINT__SENSOR_UPDATES, this::onUpdate);
        this.vertx.eventBus().consumer(Config.ENDPOINT__SENSOR_AVERAGE, this::onAverage);
    }

    private void onAverage(final Message<JsonObject> event) {
        final double average = this.sensorLastValue
            .values()
            .stream()
            .collect(Collectors.averagingDouble(Double::doubleValue));
        final JsonObject response = Util.obj(Config.FIELD__AVERAGE, average);
        event.reply(response);
    }

    private void onUpdate(final Message<JsonObject> event) {
        final String id = event.body().getString(Config.FIELD__ID);
        final double temperature = event.body().getDouble(Config.FIELD__TEMPERATURE);
        this.sensorLastValue.put(id, temperature);
    }

}
