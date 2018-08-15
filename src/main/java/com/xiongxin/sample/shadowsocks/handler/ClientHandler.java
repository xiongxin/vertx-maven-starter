package com.xiongxin.sample.shadowsocks.handler;

import com.xiongxin.sample.shadowsocks.util.LocalConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.net.*;
import io.vertx.core.net.impl.TrustAllTrustManager;
import io.vertx.core.streams.Pump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.pkcs11.Secmod;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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

  private String addr;
  private int port;

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

    client.endHandler(v -> {
      destory();
    });

    client.exceptionHandler(e -> {
      log.error("Catch Exception.", e);
      destory();
    });
  }

  private void cleanBuffer() {
    mBufferQueue = Buffer.buffer();
  }

  private void compactBuffer(int start) {
    mBufferQueue = Buffer.buffer()
      .appendBuffer(mBufferQueue.slice(start, mBufferQueue.length()));
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
    // Version + MODE + RSV + ADDR TYPE

    if (bufferLength < 4)
      return false;

    if (mBufferQueue.getByte(1) != 1) {
      log.error("Mode != 1");
      return true;
    }

    nextStage();
    // keep the addr type
    compactBuffer(3);
    log.info("data2 buffer: " + Arrays.toString(mBufferQueue.getBytes()));

    if (mBufferQueue.length() > 0) {
      return handleStageAddress();
    }

    return false;
  }

  // 分析地址
  private boolean handleStageAddress() {
    int bufferLength = mBufferQueue.length();
    String addr = null;

    // Construct the remote header
    Buffer remoteHeader = Buffer.buffer();  // 发送给远程服务器的头部
    // 0x01: IP V4 地址
    // 0x03: 域名
    // 0x04: IP V6 地址 暂时不支持
    int addrType = mBufferQueue.getByte(0);

    if (mCofig.oneTimeAuth) {
      remoteHeader.appendByte((byte) (addrType | OTA_FLAG));
    } else {
      remoteHeader.appendByte((byte) (addrType));
    }

    if (addrType ==  ADDR_TYPE_IPV4) {
      // addr type (1) + ipv4(4) + port(2)
      if (bufferLength < 7) {
        return false;
      }
      log.info("data2 address: " + Arrays.toString(mBufferQueue.getBytes(1, 5)));
      try {
        addr = InetAddress.getByAddress(mBufferQueue.getBytes(1, 5)).toString().substring(1);
      } catch (UnknownHostException e) {
        log.error("UnknownHostException . ", e);
      }

      remoteHeader.appendBytes(mBufferQueue.getBytes(1, 5));
      compactBuffer(5); // 剩下端口字节
    }

    int port = mBufferQueue.getUnsignedShort(0);

    remoteHeader.appendShort((short) port);
    compactBuffer(2);
    log.info("Connecting to " + addr + ":" + port);
    //connectToRemote(addr, port, remoteHeader);
    this.addr = addr;
    this.port = port;
    //  回复客户端
    connectToRemote(addr, port, remoteHeader);
    nextStage();
    return false;
  }

  // 发送远程请求
  private void connectToRemote(String addr, int port, Buffer remoteHeader) {
    // 5s timeout
    NetClientOptions options = new NetClientOptions().setConnectTimeout(5000);
    NetClient client = mVertx.createNetClient(options);
    client.connect(port, addr, res -> {
      if (res.succeeded()) {
        // 1. 回复客户端
        byte [] msg = {0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
        mLocalSocket.write(Buffer.buffer(msg));

        // 拿到远程数据之后返回给客户端
        mServerSocket = res.result();
        setFinishHandler(mServerSocket);
        mServerSocket.handler(buffer -> {
          flowControl(mLocalSocket, mServerSocket);
          mLocalSocket.write(buffer);
        });
      } else {
        log.error("ERROR: " + res.cause().getMessage());
      }
    });
  }


  // 获取请求数据
  private boolean handleStageData() {
    log.info("发送给远程的数据" + mBufferQueue.toString());
    mServerSocket.write(mBufferQueue);
    cleanBuffer();
    return false;
  }

  private void flowControl(NetSocket a, NetSocket b) {
    if (a.writeQueueFull()) {
      b.pause();

      a.drainHandler(done -> {
        b.resume();
      });
    }
  }


  @Override
  public void handle(Buffer buffer) {
    boolean finish = false;
    mBufferQueue.appendBuffer(buffer);
    System.out.println("mCurrentStage "+ this +" = [" + mCurrentStage + "]");
    switch (mCurrentStage) {
      case Stage.HELLO:
        finish = handleStageHello();
        break;
      case Stage.HEADER:
        finish = handleStageHeader();
        break;
      case Stage.ADDRESS:
        finish = handleStageAddress();
        break;
      case Stage.DATA:  // 通过该链接一直和客户端保持链接，接收请求进来的数据
        finish = handleStageData();
        break;
      default:
    }

    if (finish) {
      destory();
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





























