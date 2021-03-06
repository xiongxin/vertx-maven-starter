package com.xiongxin.sample.shadowsocks;

import com.xiongxin.sample.shadowsocks.util.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  public static Logger log = LoggerFactory.getLogger(Main.class);

  public static final String VERSION = "0.8.3";

  public static void main(String argv[]) {
    log.info("Shadowsocks " + VERSION);
    if (!GlobalConfig.getConfigFromArgv(argv)) {
      return;
    }

    System.out.println("argv = [" + argv + "]");

    try {
      GlobalConfig.getConfigFromFile();
    } catch (Exception e) {
      log.error("Get config from json file error.", e);
      return;
    }

    GlobalConfig.get().printConfig();

    new ShadowsockVertx(GlobalConfig.get().isServerMode()).start();
  }
}
