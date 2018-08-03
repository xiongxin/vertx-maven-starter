package com.xiongxin.sample;

public interface DatabaseConstants {
  String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";

  String DEFAULT_WIKIDB_JDBC_URL = "jdbc:mysql://172.16.0.51:3306/test?user=root&password=password&useSSL=false&useUnicode=true&characterEncoding=UTF8";
  String DEFAULT_WIKIDB_JDBC_DRIVER_CLASS = "com.mysql.jdbc.Driver";
  int DEFAULT_JDBC_MAX_POOL_SIZE = 10;
}
