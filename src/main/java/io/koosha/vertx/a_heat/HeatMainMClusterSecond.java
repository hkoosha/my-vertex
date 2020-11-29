package io.koosha.vertx.a_heat;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeatMainMClusterSecond {

    private static final Logger log = LoggerFactory.getLogger(HeatMainMClusterSecond.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), ar -> {
            if (ar.succeeded())
                deploy(ar.result());
            else
                log.error("error", ar.cause());
        });
    }

    private static void deploy(final Vertx vertx) {
        log.info("starting second instance");
        vertx.deployVerticle(HeatSensor.class.getName(), new DeploymentOptions().setInstances(4));
        vertx.deployVerticle(SensorData.class.getName());
        vertx.deployVerticle(Listener.class.getName());
        vertx.deployVerticle(HttpServer.class.getName(), new DeploymentOptions().setConfig(
            new JsonObject().put(Config.CFG__PORT, Config.DEFAULT_PORT + 1)
        ));
    }

}
