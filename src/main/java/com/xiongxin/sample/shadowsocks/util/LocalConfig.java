package com.xiongxin.sample.shadowsocks.util;

import java.net.InetSocketAddress;

public class LocalConfig{
  public String password;
  public String method;
  public String server;
  public int serverPort;
  public int localPort;
  public boolean oneTimeAuth;
  public int timeout;
  // For server is target, for local is server.
  public InetSocketAddress remoteAddress;

  public String target;

  public LocalConfig(String k, String m, String s, int p, int lp, boolean ota, int t){
    password = k;
    method = m;
    server = s;
    serverPort = p;
    localPort = lp;
    oneTimeAuth = ota;
    timeout = t;
  }
}
