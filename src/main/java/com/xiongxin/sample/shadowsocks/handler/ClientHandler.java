package com.xiongxin.sample.shadowsocks.handler;

import com.xiongxin.sample.shadowsocks.util.LocalConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * 捕获客户端发送的链接
 */
public class ClientHandler implements Handler<Buffer> {
  private static Logger log = LoggerFactory.getLogger(ClientHandler.class);

  private final static int ADDR_TYPE_IPV4 = 1;
  private final static int ADDR_TYPE_HOST = 3;

  private final static int OTA_FLAG = 0x10;
  private int mCurrentStage;
  private NetSocket mLocalSocket;
  private NetSocket mServerSocket;
  private Buffer mBufferQueue;
  private Vertx mVertx;
  private LocalConfig mCofig;
  private int mChunkCount;

  private class Stage {
    final public static int HELLO = 0;
    final public static int HEADER = 1;
    final public static int ADDRESS = 2;
    final public static int DATA = 3;
    final public static int DESTORY = 100;
  }

  private void nextStage() {
    if (mCurrentStage != Stage.DATA) {
      mCurrentStage++;
    }
  }

  public ClientHandler(Vertx vertx, NetSocket socket, LocalConfig config) {
    mVertx = vertx;
    mCofig = config;
    mLocalSocket = socket;      // 浏览器发送过来的Socket
    mCurrentStage = Stage.HELLO;
    mBufferQueue = Buffer.buffer();
    mChunkCount = 0;
    setFinishHandler(mLocalSocket);
  }

  // When any sockets meet close/end/exception, destory the others.
  private void setFinishHandler(NetSocket client) {
    client.closeHandler(v -> {
      destory();
    });
  }

  private Buffer cleanBuffer() {
    mBufferQueue = Buffer.buffer();
    return mBufferQueue;
  }

  // 第一次链接
  private boolean handleStageHello() {
    log.info("data1：" + Arrays.toString(mBufferQueue.getBytes()));

    int bufferLength = mBufferQueue.length();
    // VERSION + METHOD LEN + METHOD
    if (bufferLength < 3)
      return false;

    // SOCK5
    if (mBufferQueue.getByte(0) != 5) {
      log.warn("Protocol error.");
      return true;
    }

    int methodLen = mBufferQueue.getByte(1);
    if (bufferLength < methodLen + 2)
      return false;

    byte[] msg = {0x05, 0x00};
    mLocalSocket.write(Buffer.buffer(msg));
    // Discard the method list
    cleanBuffer();
    nextStage();

    return false;
  }

  // 第二次连接
  private boolean handleStageHeader() {
    log.info("data2：" + Arrays.toString(mBufferQueue.getBytes()));

    int bufferLength = mBufferQueue.length();


  }

  @Override
  public void handle(Buffer buffer) {
    boolean finish = false;
    mBufferQueue.appendBuffer(buffer);

    switch (mCurrentStage) {
      case Stage.HELLO:
        finish = handleStageHello();
        break;
      case Stage.HEADER:

    }
  }


  private synchronized void destory() {
    if (mCurrentStage != Stage.DESTORY) {
      mCurrentStage = Stage.DESTORY;
    }

    if (mLocalSocket != null) mLocalSocket.close();
    if (mServerSocket != null) mServerSocket.close();
  }
}





























