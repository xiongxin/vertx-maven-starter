package com.xiongxin.sample.memcache;

public enum Status {
  NO_ERROR(0, "NO_ERROR"),
  KEY_NOT_FOUND(1, "KEY_NOT_FOUND");

  private int status;
  private String name;

  private Status(int status, String name) {
    this.status = status;
    this.name = name;
  }
}
