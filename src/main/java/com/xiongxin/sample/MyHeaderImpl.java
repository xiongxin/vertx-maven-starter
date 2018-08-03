package com.xiongxin.sample;

import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyHeaderImpl implements CookieHandler {
  private static Logger logger = LoggerFactory.getLogger(MyHeaderImpl.class);

  @Override
  public void handle(RoutingContext event) {
    logger.info("捕获请求...");
    event.next();
  }
}
