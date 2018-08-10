package com.xiongxin.sample.tcp;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

public class TcpClientSample extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    NetClientOptions options = new NetClientOptions().setConnectTimeout(5000);
    NetClient client = vertx.createNetClient(options);
    client.connect(8802, "172.16.0.51", res -> {
      if (res.succeeded()) {
        System.out.println("Connected");
        NetSocket socket = res.result();
        socket.write("set foo 0 900 5\r\nvalue\r\n");
        socket.handler(buf -> {
          //System.out.println(buf.toString());

          if ( buf.toString().equals("STORED\r\n") ) {
            socket.write("get foo\r\n");
          } else {
            System.out.println("========" + buf.toString());
          }
        });
      } else {
        System.out.println("Failed to connect: " + res.cause().getMessage());
      }
    });
  }
}
