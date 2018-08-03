package com.xiongxin.sample.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WikiDatabaseServiceImpl implements WikiDatabaseService {
  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDatabaseServiceImpl.class);

  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  WikiDatabaseServiceImpl(JDBCClient dbClient, HashMap<SqlQuery,String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;
    LOGGER.info("WikiDatabaseServiceImpl创建");
    readyHandler.handle(Future.succeededFuture(this));
  }


  /**
   * 查询所有页面的名称
   * @param resultHandler 回调函数
   * @return WikiDatabaseService
   */
  @Override
  public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES), res -> {
      if (res.succeeded()) {
        JsonArray result = new JsonArray(res.result()
          .getResults()
          .stream()
          .map(json -> json.getString(0))
          .sorted()
          .collect(Collectors.toList()));

        resultHandler.handle(Future.succeededFuture(result));
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), new JsonArray().add(name), res -> {
      if (res.succeeded()) {
        JsonObject jsonObject = new JsonObject();
        ResultSet resultSet = res.result();
        if (resultSet.getNumRows() == 0) {
          jsonObject.put("found", false);
        } else {
          jsonObject.put("found", true);
          JsonArray row = resultSet.getResults().get(0);
          jsonObject.put("id", row.getInteger(0));
          jsonObject.put("rawContent", row.getString(1));
        }
        resultHandler.handle(Future.succeededFuture(jsonObject));
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), new JsonArray().add(title).add(markdown), res -> {
      if (res.succeeded()) {
        LOGGER.info("创建数据成功");
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(markdown).add(id);

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.SAVE_PAGE), params, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(id);

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), params, res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES_DATA), res -> {
      if (res.succeeded()) {
        resultHandler.handle(Future.succeededFuture(res.result().getRows()));
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }

  @Override
  public WikiDatabaseService fetchPageById(int id, Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE_BY_ID), new JsonArray().add(id), res -> {
      if (res.succeeded()) {
        JsonObject jsonObject = new JsonObject();
        ResultSet resultSet = res.result();
        if (resultSet.getNumRows() == 0) {
          jsonObject.put("found", false);
        } else {
          jsonObject.put("found", true);
          JsonArray row = resultSet.getResults().get(0);
          jsonObject.put("id", row.getInteger(0));
          jsonObject.put("name", row.getString(1));
          jsonObject.put("content", row.getString(2));
        }

        resultHandler.handle(Future.succeededFuture(jsonObject));
      } else {
        LOGGER.error("Database query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });

    return this;
  }
}





















