package com.xiongxin.sample;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  @Override
  public void start(Future<Void> startFuture) {
    LOGGER.info("start...");
    Future<String> dbVerticleDeployment = Future.future();

    String json = "[\"sfasd@qq.com\", \"sdfsdf@qq.com\",\"afsdfasd@qqq.com\"]";
    JsonArray jsonArray = new JsonArray(json);
    System.out.println("jsonArray = [" + json + "]");

    vertx.deployVerticle(new WikiDatabaseVerticle(), dbVerticleDeployment.completer());

    dbVerticleDeployment.compose(id -> {
      LOGGER.info("db" + id);
      Future<String> httpVerticleDeployment = Future.future();

      vertx.deployVerticle(
        "com.xiongxin.sample.HttpServerVerticle",
        new DeploymentOptions().setInstances(1),
        httpVerticleDeployment.completer());

      return httpVerticleDeployment;

    }).setHandler(ar -> {
      if (ar.succeeded()) {
        LOGGER.info("服务器启动成功");
        startFuture.complete();
      } else {
        LOGGER.error("服务器启动失败");
        startFuture.fail(ar.cause());
      }
    });
  }
}








































































