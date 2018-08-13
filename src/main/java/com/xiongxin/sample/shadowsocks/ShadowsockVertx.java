package com.xiongxin.sample.shadowsocks;

import com.xiongxin.sample.shadowsocks.handler.ClientHandler;
import com.xiongxin.sample.shadowsocks.util.GlobalConfig;
import com.xiongxin.sample.shadowsocks.util.LocalConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShadowsockVertx {
  private static Logger log = LoggerFactory.getLogger(ShadowsockVertx.class);

  private Vertx mVertx;
  private boolean mIsServer;
  private NetServer mNetServer;

  public ShadowsockVertx(boolean isServer) {
    mVertx = Vertx.vertx();
    mIsServer = isServer;
  }

  public void start() {
    // 读取配置文件
    LocalConfig config = GlobalConfig.createLocalConfig();
    int port = mIsServer ? config.serverPort : config.localPort;

    mNetServer = mVertx.createNetServer().connectHandler(client -> { // 捕获客户端链接
      Handler<Buffer> bufferHandler = new ClientHandler(mVertx, client,config);
      client.handler(bufferHandler);
    }).listen(port, "0.0.0.0", res -> {
      if (res.succeeded()) {
        log.info("Listening at " + port);
      } else {
        log.error("Start failed!" + res.cause().getMessage());
      }
    });
  }
}
