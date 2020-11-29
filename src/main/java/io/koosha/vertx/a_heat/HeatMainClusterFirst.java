package io.koosha.vertx.a_heat;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeatMainClusterFirst extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(HeatMainClusterFirst.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions(), ar -> {
            if (ar.succeeded())
                deploy(ar.result());
            else
                log.error("error", ar.cause());
        });
    }

    private static void deploy(final Vertx vertx) {
        log.info("starting first instance");
        vertx.deployVerticle(HeatSensor.class.getName(), Util.instances(4));
        vertx.deployVerticle(HttpServer.class.getName());
    }

}
