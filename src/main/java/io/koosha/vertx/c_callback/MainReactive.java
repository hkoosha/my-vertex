package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.vertx.core.Vertx;

public final class MainReactive {

    public static void main(final String... args) {
        final Vertx vertx = Util.deploy(SnapshotService.class, CollectorServiceRx.class);

        for (int i = 0; i < 3; i++)
            vertx.deployVerticle(HeatSensor.class.getName(),
                Util.configed(Config.CFG__PORT, Config.SENSOR_PORT_BASE + i));
    }

}
