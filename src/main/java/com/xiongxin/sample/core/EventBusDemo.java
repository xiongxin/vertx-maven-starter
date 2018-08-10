package com.xiongxin.sample.core;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

public class EventBusDemo {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    EventBus eventBus = vertx.eventBus();

    MessageConsumer<String> consumer = eventBus.consumer("news.uk.sport");

//    consumer.handler(message -> {
//      System.out.println("I have received a message1111:  = [" + message.body() + message.headers() + "]");
//    });

    eventBus.consumer("news.uk.sport",message -> {
      System.out.println("I have received a message22222:  = [" + message.body() + message.headers() + "]");
      message.reply("回执信息");
    });

    consumer.completionHandler(res -> {
      if (res.succeeded()) {
        System.out.println("res = [" + "完成接受信息" + "]");
      } else {
        System.out.println("res = [" + "接受信息失败" + "]");
      }
    });

    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("a", "a1");
    eventBus.send("news.uk.sport", "Yay! Someone kicked a ball", reply -> {
      System.out.println("reply = [接收回执信息" + reply.result().body() + "]");
    });
    //eventBus.publish("news.uk.sport", "This is a message",options);
  }
}
