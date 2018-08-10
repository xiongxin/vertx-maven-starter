package com.xiongxin.sample.core;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

public class ParserSample {
  public static void main(String[] args) {
    final RecordParser parser = RecordParser.newDelimited("\n", buf -> {
      System.out.println("buf = [" + buf + "]");
    });

    parser.handle(Buffer.buffer("HELLO\nHOW ARE Y"));
    parser.handle(Buffer.buffer("OU?\nI AM"));
    parser.handle(Buffer.buffer("DOING OK"));
    parser.handle(Buffer.buffer("\n"));
  }
}
