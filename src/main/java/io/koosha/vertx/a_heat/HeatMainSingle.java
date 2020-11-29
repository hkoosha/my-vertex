package io.koosha.vertx.a_heat;

import io.koosha.vertx.Util;
import io.vertx.core.Vertx;

public class HeatMainSingle {

    public static void main(String... args) {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(HeatSensor.class.getName(), Util.instances(4));
        vertx.deployVerticle(Listener.class.getName());
        vertx.deployVerticle(SensorData.class.getName());
        vertx.deployVerticle(HttpServer.class.getName());
    }

}
