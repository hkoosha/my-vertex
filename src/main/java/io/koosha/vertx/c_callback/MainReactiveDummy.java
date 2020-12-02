package io.koosha.vertx.c_callback;

import io.koosha.vertx.Util;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class MainReactiveDummy {

    private static final Logger log = LoggerFactory.getLogger(MainReactiveDummy.class);

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(final String... args) {
        Observable.just(1, 2, 3)
                  .map(it -> "@" + it)
                  .subscribe(log::info);


        log.info("~~~~~\n\n------------------");
        Observable.error(() -> new RuntimeException("blown up"))
                  .map(Object::toString)
                  .map(String::toUpperCase)
                  .subscribe(log::info, err -> log.error("err: " + err.getMessage()));


        log.info("~~~~~\n\n------------------");
        Observable.just("--", "this", "is", "--", "items", "here")
                  .doOnSubscribe(it -> log.info("subscribed 0"))
                  .delay(2, TimeUnit.SECONDS)
                  .filter(it -> !it.startsWith("--"))
                  .doOnNext(log::info)
                  .map(String::toUpperCase)
                  .buffer(2)
                  .subscribe(all -> log.info("all: {}", all), err -> log.error("err", err), () -> log.info("fin"));

        Util.sleepS(3);
    }

}
