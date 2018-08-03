package com.xiongxin.sample;

import com.xiongxin.sample.database.WikiDatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import com.xiongxin.sample.database.SqlQuery;

public class WikiDatabaseVerticle extends AbstractVerticle {
  public static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resource.file";

  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDatabaseVerticle.class);

  private JDBCClient dbClient;

  private final HashMap<SqlQuery, String> sqlQueries = new HashMap<>();

  private HashMap<SqlQuery, String> loadSqlQueries() throws IOException {
    String queriesFile = config().getString(CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE);
    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = new FileInputStream(queriesFile);
    } else {
      queriesInputStream = getClass().getResourceAsStream("/db-queries.properties");
    }

    Properties queriesProps = new Properties();
    queriesProps.load(queriesInputStream);
    queriesInputStream.close();

    sqlQueries.put(SqlQuery.ALL_PAGES, queriesProps.getProperty("all-pages"));
    sqlQueries.put(SqlQuery.GET_PAGE, queriesProps.getProperty("get-page"));
    sqlQueries.put(SqlQuery.CREATE_PAGE, queriesProps.getProperty("create-page"));
    sqlQueries.put(SqlQuery.SAVE_PAGE, queriesProps.getProperty("save-page"));
    sqlQueries.put(SqlQuery.DELETE_PAGE, queriesProps.getProperty("delete-page"));
    sqlQueries.put(SqlQuery.ALL_PAGES_DATA, queriesProps.getProperty("all-pages-data"));
    sqlQueries.put(SqlQuery.GET_PAGE_BY_ID, queriesProps.getProperty("get-page-by-id"));

    return sqlQueries;
  }

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    HashMap<SqlQuery, String> sqlQueries =  loadSqlQueries(); // 加载sql语句

    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
      .put("jdbcUrl", "jdbc:mysql://172.16.0.51:3306/test")
      .put("driver_class", "com.mysql.jdbc.Driver")
      .put("maxPoolSize", 10)
      .put("username", "root")
      .put("password", "root")
    );

    WikiDatabaseService.create(dbClient, sqlQueries, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder.setAddress(CONFIG_WIKIDB_QUEUE)
          .register(WikiDatabaseService.class, ready.result());  // 将WikiDatabaseServiceImpl注册到EventBus
                                                                 // 可以使用代理类来请求到EventBus转发请求

        startFuture.complete();
      } else {
        startFuture.fail(ready.cause());
      }
    });
  }
}
