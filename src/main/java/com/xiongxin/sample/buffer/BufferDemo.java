package com.xiongxin.sample.buffer;

import io.vertx.core.buffer.Buffer;

public class BufferDemo {
  public static void main(String[] args) {
    Buffer buffer = Buffer.buffer();

    buffer.setInt(1000, 123);
    buffer.setString(0,"hello");

    System.out.println(" buffer.length()  = [" + buffer.length() + "]");
    System.out.println("args = [" + buffer.getString(0, 5) + "]");
    System.out.println("args = [" + buffer.getInt(1000) + "]");
  }
}
