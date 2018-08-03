package com.xiongxin.sample;

import com.github.rjeschke.txtmark.Processor;
import com.xiongxin.sample.database.WikiDatabaseService;
import com.xiongxin.sample.database.WikiPerms;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);
  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";
  private static final String EMPTY_PAGE_MARKDOWN =
    "# A new page\n" +
      "\n" +
      "Feel-free to write in Markdown!\n";

  private String wikiDbQueue = "wikidb.queue";

  private WikiDatabaseService dbService;

  @Override
  public void start(Future<Void> startFuture) {
    LOGGER.info("start http server");
    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);

    JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
      .put("jdbcUrl", "jdbc:mysql://172.16.0.51:3306/test")
      .put("driver_class", "com.mysql.jdbc.Driver")
      .put("maxPoolSize", 10)
      .put("username", "root")
      .put("password", "root"));


    HttpServer server = vertx.createHttpServer();
    JDBCAuth auth = JDBCAuth.create(vertx, dbClient);
    JWTAuth jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .setKeyStore(new KeyStoreOptions().setPath("/home/xiongxin/Application/Code/java-apps/vertx-maven-starter/keystore.jceks")
        .setType("jceks").setPassword("secret")));

    Router router = Router.router(vertx);

//    JDBCHashStrategy strategy = JDBCHashStrategy.createSHA512(vertx);
//    JsonArray row = new JsonArray()
//      .add("C705F9EAD3406D0C17DDA3668A365D8991E6D1050C9A41329D9C67FC39E53437A39E83A9586E18C49AD10E41CBB71F0C06626741758E16F9B6C2BA4BEE74017E")
//      .add("017DC3D7F89CD5E873B16E6CCE9A2307C8E3D9C5758741EEE49A899FFBC379D8");
//    String hashedStoredPwd = strategy.getHashedStoredPwd(row);
//    String salt = strategy.getSalt(row);
//    LOGGER.info("hashedStoredPwd==========" + hashedStoredPwd);
//    LOGGER.info("salt==========" + salt);
//    int version = -1;
//    int sep = hashedStoredPwd.lastIndexOf('$');
//    LOGGER.info("sep============="+sep);
//    String hashedPassword = strategy.computeHash("admin", salt, version);
//    LOGGER.info("hashedPassword============="+hashedPassword);
//    LOGGER.info("hashedPassword============="+hashedPassword.equals(hashedStoredPwd));


    router.route().handler(MyHeader.create());
    router.route().handler(CookieHandler.create());
    router.route().handler(BodyHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(UserSessionHandler.create(auth));
    // AUTH BEGIN
    AuthHandler authHandler = RedirectAuthHandler.create(auth, "/login");
    router.get("/").handler(authHandler);
    router.get("/wiki/*").handler(authHandler);
    router.get("/action/*").handler(authHandler);
    // AUTH END
    router.get("/").handler(this::indexHandler);

    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post("/action/create").handler(this::pageCreateHandler);
    router.post("/action/save").handler(this::pageUpdateHandler);
    router.post("/action/delete").handler(this::pageDeletionHandler);
    router.post("/action/backup").handler(this::backupHandler);

    router.get("/login").handler(this::loginHandler);
    router.post("/login").handler(FormLoginHandler.create(auth));
    router.get("/logout").handler(context -> {
      context.clearUser();
      context.response()
        .setStatusCode(302)
        .putHeader("Location", "/")
        .end();
    });

    Router apiRouter = Router.router(vertx);
    apiRouter.route().handler(JWTAuthHandler.create(jwtAuth, "/api/token"));
    apiRouter.get("/token").handler(context -> {
      JsonObject creds = new JsonObject()
        .put("username", context.request().getHeader("login"))
        .put("password", context.request().getHeader("password"));

      auth.authenticate(creds, authResult -> {
        if (authResult.succeeded()) {
          User user = authResult.result();
          user.isAuthorized("wiki:create",
            canWikiCreate -> user.isAuthorized("wiki:delete",
              canWikiDelete -> user.isAuthorized("wiki:update",
                canWikiUpdate -> {
                  String token = jwtAuth.generateToken(
                    new JsonObject()
                      .put("username", context.request().getHeader("login"))
                      .put("canWikiCreate", canWikiCreate.succeeded() && canWikiUpdate.result())
                      .put("canWikiDelete", canWikiDelete.succeeded() && canWikiDelete.result())
                      .put("canWikiUpdate", canWikiUpdate.succeeded() && canWikiUpdate.result()),   // jwt user principal object
                    new JWTOptions()
                      .setSubject("Wiki API")
                      .setIssuer("Vert.x")
                  );

                  context.response().putHeader("Content-type", "text/plain").end(token);
          })));
        } else {
          context.response().setStatusCode(401).end();
        }
      });
    });
    apiRouter.get("/pages").handler(this::apiRoot);
    apiRouter.get("/pages/:id").handler(this::apiGetPage);
    apiRouter.post("/pages").handler(this::apiCreatePage);
    apiRouter.put("/pages/:id").handler(this::apiUpdatePage);
    apiRouter.delete("/pages/:id").handler(this::apiDeletePage);
    router.mountSubRouter("/api", apiRouter);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8802);
    server.requestHandler(router::accept)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + portNumber);
          startFuture.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          startFuture.fail(ar.cause());
        }
      });
  }


  /**
   * Login页面
   */
  private void loginHandler(RoutingContext context) {
    context.put("title", "Login");
    templateEngine.render(context, "templates", "/login.ftl", ar -> {
      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html");
        context.response().end(ar.result());
      } else {
        context.fail(ar.cause());
      }
    });
  }

  /**
   * 首页
   * @param context routing
   */
  private void indexHandler(RoutingContext context) {
    context.user().isAuthorized("wiki:create", res -> {
      boolean canWikiCreate = res.succeeded() && res.result();

      dbService.fetchAllPages( reply -> {
        if (reply.succeeded()) {
          context.put("title", "Wiki home");
          context.put("pages", reply.result().getList());
          context.put("canWikiCreate", canWikiCreate);
          context.put("username", context.user().principal().getString("username"));

          templateEngine.render(context, "templates", "/index.ftl", ar -> {
            if (ar.succeeded()) {
              context.response().putHeader("Content-Type", "text/html");
              context.response().end(ar.result());
            } else {
              context.fail(ar.cause());
            }
          });
        } else {
          context.fail(reply.cause());
        }
      });
    });

  }

  /**
   * wiki页面
   */
  private void pageRenderingHandler(RoutingContext context) {
    String page = context.request().getParam("page");

    dbService.fetchPage(page,  reply -> {
      if (reply.succeeded()) {
        JsonObject body = reply.result();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);

        context.put("title", page);
        context.put("id", body.getInteger("id", -1));
        context.put("newPage", found ? "no" : "yes");
        context.put("rawContent", rawContent);
        context.put("content", Processor.process(rawContent));
        context.put("timestamp", new Date().toString());

        templateEngine.render(context, "templates", "/page.ftl", ar -> {
          if (ar.succeeded()) {
            context.response().putHeader("Content-type", "text/html");
            context.response().end(ar.result());
          } else {
            context.fail(ar.cause());
          }
        });
      } else {
        context.fail(reply.cause());
      }
    });
  }

  /**
   * 新建的页面展示接口
   */
  private void pageCreateHandler(RoutingContext context) {
    String pageName = context.request().getParam("name");
    String location = "/wiki/" + pageName;

    if (pageName == null || pageName.isEmpty()) {
      location = "/";
    }

    context.response().setStatusCode(303); // 重定向
    context.response().putHeader("Location", location);
    context.response().end();
  }

  /**
   * 创建或更新wiki
   */
  private void pageUpdateHandler(RoutingContext context) {
    // 请求参数
    String id = context.request().getParam("id");
    String title = context.request().getParam("title");
    String markdown = context.request().getParam("markdown");
    boolean newPage = "yes".equals(context.request().getParam("newPage"));

    // 相应结果处理函数
    Handler<AsyncResult<Void>> handler = reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/wiki/" + title);
        context.response().end();
      } else {
        context.fail(reply.cause());
      }
    };

    if (newPage) dbService.createPage(title, markdown, handler);
    else dbService.savePage(Integer.valueOf(id), markdown, handler);
  }

  /**
   * 删除wiki
   */
  private void pageDeletionHandler(RoutingContext context) {
    // 请求参数
    String id = context.request().getParam("id");

    // 处理mysql处理结果
    dbService.deletePage(Integer.valueOf(id), reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(303);
        context.response().putHeader("Location", "/");
        context.response().end();
      } else {
        context.fail(reply.cause());
      }
    });
  }

  private void backupHandler(RoutingContext context) {
    dbService.fetchAllPagesData(reply -> {
      if (reply.succeeded()) {
        JsonArray filesObject = new JsonArray();
        JsonObject payload = new JsonObject()
          .put("files", filesObject)
          .put("language", "plaintext")
          .put("title", "vertx-wiki-backup")
          .put("public", true);

        reply.result()
          .forEach(page -> {
            JsonObject fileObject = new JsonObject();
            fileObject.put("name", page.getString("Name"));
            fileObject.put("content", page.getString("Content"));
            filesObject.add(fileObject);
          });

        context.response().setStatusCode(200);
        context.response().end(payload.encode());
      } else {
        context.fail(reply.cause());
      }
    });
  }

  /**
   * 首页接口
   */
  private void apiRoot(RoutingContext context) {
    dbService.fetchAllPagesData(reply -> {
      JsonObject response = new JsonObject();
      if (reply.succeeded()) {
        List<JsonObject> pages = reply.result()
          .stream()
          .map(obj -> new JsonObject()
            .put("id", obj.getInteger("Id"))
            .put("name", obj.getString("Name")))
          .collect(Collectors.toList());

        response
          .put("success", true)
          .put("pages", pages);
        context.response().setStatusCode(200);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(response.encode());
      } else {
        response
          .put("success", false)
          .put("error", reply.cause().getMessage());
        context.response().setStatusCode(500);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(response.encode());
      }
    });
  }

  private void apiGetPage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    dbService.fetchPageById(id, reply -> {
      JsonObject response = new JsonObject();
      if (reply.succeeded()) {
        JsonObject dbObject = reply.result();
        if (dbObject.getBoolean("found")) {
          JsonObject payload = new JsonObject()
            .put("name", dbObject.getString("name"))
            .put("id", dbObject.getInteger("id"))
            .put("markdown", dbObject.getString("content"))
            .put("html", Processor.process(dbObject.getString("content")));
          response
            .put("success", true)
            .put("page", payload);
          context.response().setStatusCode(200);
        } else {
          context.response().setStatusCode(404);
          response
            .put("success", false)
            .put("error", "There is no page with ID " + id);
        }
      } else {
        response
          .put("success", false)
          .put("error", reply.cause().getMessage());
        context.response().setStatusCode(500);
      }
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(response.encode());
    });
  }

//    apiRouter.post("/pages").handler(this::apiCreatePage);
//    apiRouter.put("/pages/:id").handler(this::apiUpdatePage);
//    apiRouter.delete("/pages/:id").handler(this::apiDeletePage);

  private void apiCreatePage(RoutingContext context) {
    JsonObject page = context.getBodyAsJson();
    if (!validateJsonPageDocument(context, page, "name", "markdown")) {
      return;
    }
    dbService.createPage(page.getString("name"), page.getString("markdown") , reply -> {
      if (reply.succeeded()) {
        context.response().setStatusCode(201);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(new JsonObject().put("success", true).encodePrettily());
      } else {
        context.response().setStatusCode(500);
        context.response().putHeader("Content-Type", "application/json");
        context.response().end(new JsonObject()
          .put("success", false)
          .put("error", reply.cause().getMessage()).encodePrettily());
      }
    });
  }

  private void apiUpdatePage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    JsonObject page = context.getBodyAsJson();
    if (!validateJsonPageDocument(context, page, "markdown")) {
      return;
    }

    dbService.savePage(id, page.getString("markdown"), reply -> {
      handleSimpleDbReply(context, reply);
    });
  }

  private void apiDeletePage(RoutingContext context) {
    int id = Integer.valueOf(context.request().getParam("id"));
    dbService.deletePage(id, reply -> {
      handleSimpleDbReply(context, reply);
    });
  }

  private void handleSimpleDbReply(RoutingContext context, AsyncResult<Void> reply) {
    if (reply.succeeded()) {
      context.response().setStatusCode(200);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject().put("success", true).encode());
    } else {
      context.response().setStatusCode(500);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", reply.cause().getMessage()).encode());
    }
  }

  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.error("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encodePrettily());

      return false;
    }

    return true;
  }
}
