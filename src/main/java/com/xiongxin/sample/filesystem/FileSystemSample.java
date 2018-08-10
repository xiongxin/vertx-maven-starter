package com.xiongxin.sample.filesystem;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;

public class FileSystemSample {
  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.fileSystem().open("target/classes/hello.txt", new OpenOptions(), result -> {
      if (result.succeeded()) {
        AsyncFile file = result.result();
        Buffer buff = Buffer.buffer("foo");
        for (int i = 0; i < 5; i++) {
          file.write(buff, buff.length() * i, ar -> {
            if (ar.succeeded()) {
              System.out.println("Written ok!");
              // etc
            } else {
              System.err.println("Failed to write: " + ar.cause());
            }
          });
        }
      } else {
        System.err.println("Cannot open file " + result.cause());
      }
    });
  }
}
