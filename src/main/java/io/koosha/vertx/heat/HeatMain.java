package io.koosha.vertx.heat;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class HeatMain {

    public static void main(String... args) {
        final Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setInstances(4));
        vertx.deployVerticle(Listener.class.getName());
        vertx.deployVerticle(SensorData.class.getName());
        vertx.deployVerticle(HttpServer.class.getName());
    }

}
