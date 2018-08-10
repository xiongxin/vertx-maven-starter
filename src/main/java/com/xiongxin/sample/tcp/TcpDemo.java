package com.xiongxin.sample.tcp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.NetServer;

public class TcpDemo extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) throws Exception {

//    NetServerOptions options = new NetServerOptions();
//    options.setHost("127.0.0.1");
//    options.setPort(1234);

    NetServer server = vertx.createNetServer();

    // handle client connect
    server.connectHandler(client -> {
      System.out.println("client = [" + client + "]");
      System.out.println("client.localAddress()=" + client.localAddress());   // Server Address
      System.out.println("client.remoteAddress()=" + client.remoteAddress()); // Client Address

      client.close();

      client.handler(buffer -> {
        System.out.println("client.handler = [" + buffer.toString() + "]");
        client.write(buffer.toString());
      });

      client.closeHandler(event -> {
        System.out.println("client.closeHandler = [" + "client is closed" + "]");
      });
    });

    // listen client
    server.listen( res -> {
      if (res.succeeded()) {
        System.out.println("Server is now listening!" + res.result().actualPort());
        startFuture.complete();
      } else {
        System.out.println("Failed to bind!");
        startFuture.fail(res.cause());
      }
    });
  }
}
