package io.koosha.vertx.heat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public final class Listener extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    private static final DecimalFormat formatter = new DecimalFormat("#.##");

    public void start() {
        this.vertx.eventBus().consumer(Config.ENDPOINT__SENSOR_UPDATES, this::handle);
    }

    private void handle(final Message<JsonObject> event) {
        final String id = event.body().getString(Config.FIELD__ID);
        final double temperature = event.body().getDouble(Config.FIELD__TEMPERATURE);
        final String formatted = formatter.format(temperature);
        log.info("id={} temperature={}", id, formatted);
    }

}
