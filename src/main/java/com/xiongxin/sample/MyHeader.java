package com.xiongxin.sample;


import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CookieHandler;

public interface MyHeader extends Handler<RoutingContext> {
  static CookieHandler create() {
    return new MyHeaderImpl();
  }
}
