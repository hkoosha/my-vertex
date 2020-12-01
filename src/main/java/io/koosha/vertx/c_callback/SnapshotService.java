package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SnapshotService extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    @Override
    public void start(final Promise<Void> startPromise) {
        final int port = config().getInteger(Config.CFG__PORT, Config.SNAPSHOT_PORT);

        vertx.createHttpServer()
             .requestHandler(req -> {
                 if (!req.method().equals(HttpMethod.POST) ||
                     !Util.HEADER__CONTENT_TYPE__JSON.equals(req.getHeader(Util.HEADER__CONTENT_TYPE))) {
                     req.response().setStatusCode(400).end();
                 }
                 else {
                     req.bodyHandler(buffer -> {
                         log.info("latest temperature: {}", buffer.toJsonObject().encodePrettily());
                         req.response().end();
                     });
                 }
             })
             .listen(port)
             .onSuccess(event -> {
                 log.info("snapshot server started on: {}", port);
                 startPromise.complete();
             })
             .onFailure(event -> {
                 log.error("failed to start snapshot server on {}", port);
                 startPromise.fail(event.getCause());
             });
    }

}
