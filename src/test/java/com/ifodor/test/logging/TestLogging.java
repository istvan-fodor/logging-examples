package com.ifodor.test.logging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TestLogging {

  private static final Logger log = LoggerFactory.getLogger(TestLogging.class);
  private static DataSource postgres;
  private static DataSource hsql;

  @BeforeClass
  public static void init() throws InterruptedException, SQLException, IOException {
    hsql = initHsql();
    postgres = initPostgres();
    log.info(
        "TestLogging output start\n-------------------------------------------------------------------------------");
  }

  @AfterClass
  public static void shutdown() throws SQLException {
    hsql.getConnection().createStatement().execute("SHUTDOWN");
    log.info("TestLogging output end\n-------------------------------------------------------------------------------");
  }

  protected static DataSource initHsql() throws SQLException, IOException {
    log.trace("Initializeing HSQL");
    return initDB("db.hsql.jdbcurl", "db.hsql.driverClass", "db.hsql.username", "db.hsql.password", "/hsqldb.sql");
  }

  protected static DataSource initPostgres() throws SQLException, IOException {
    log.trace("Initializeing POSTGRES");
    return initDB("db.pg.jdbcurl", "db.pg.driverClass", "db.pg.username", "db.pg.password", "/postgresql.sql");
  }

  protected static DataSource initDB(String jdbcUrlKey, String driverClassKey, String usernameKey, String passwordKey,
      String dbScript) throws SQLException, IOException {
    Properties props = new Properties();
    InputStream resourceAsStream = TestLogging.class.getClass().getResourceAsStream("/db.properties");
    props.load(resourceAsStream);
    IOUtils.closeQuietly(resourceAsStream);

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(props.getProperty(jdbcUrlKey));
    config.setDriverClassName(props.getProperty(driverClassKey));
    config.setUsername(props.getProperty(usernameKey));
    config.setPassword(props.getProperty(passwordKey));

    HikariDataSource ds = new HikariDataSource(config);
    Connection connection = ds.getConnection();

    InputStream in = TestLogging.class.getClass().getResourceAsStream(dbScript);
    String sql = IOUtils.toString(in, Charset.defaultCharset());
    IOUtils.closeQuietly(in);
    for (String ddl : sql.split(";")) {
      // log.info("\n{}", ddl);
      connection.createStatement().execute(ddl);
    }
    return ds;
  }

  @Test
  public void testLevels() {
    log.trace("This is a Trace leve log message");
    log.debug("This is a Debug level log message");
    log.info("This is an Info level log message");
    log.warn("This is a Warn leve log message");
    log.error("This is an {} {} {}", "Error", "level", "log message");
  }

  @Test
  public void testBigObject() {
    log.info("Hello {}!", "World");

    Object bigObject = new Object() {
      public String toString() {
        try {
          Thread.sleep(1000l);
        } catch (InterruptedException e) {
          log.error("Interrupted.. too bad", e);
        }
        return "I'm big!";
      }
    };
    // OK
    log.trace("Big Object: {}", bigObject);
    log.warn("Big Object: {}", bigObject);

    /*
     * OK in edge cases. Example would be that log message needs
     * Regex manipulation before being logged.
     */
    if (log.isTraceEnabled()) {
      log.trace(bigObject.toString().replace("!", "?"));
    }

    // Not OK! - toString is not under our control, we don't always know the implementation details
    // log.trace("Big Object: {}", bigObject.toString());
    // log.trace("Big Object: " + bigObject.toString());
  }

  /*
   * Useful for user tracking, corelation ids
   */
  @Test
  public void testMdc() {
    new Thread() {
      @Override
      public void run() {
        MDC.put("callerId", "Istvan");
        log.info("Hello Contextualized {}!", "World");
      }
    }.start();

    MDC.put("callerId", "Not_Istvan");
    log.info("Hello Contextualized {}!", "World");
    MDC.clear();
  }

  @Test
  public void testMdcFile() {
    Logger log = LoggerFactory.getLogger("mdc-logger");

    MDC.put("callerId", "A");
    log.info("Hello A!");

    MDC.put("callerId", "B");
    log.info("Hello B");

    MDC.clear();

    log.info("Who are you?");
    /*
    IntStream.range(1, 10001).map(i -> ThreadLocalRandom.current().nextInt(0, 3)).boxed().map(r -> {
      String ret = null;
      switch (r) {
        case 0:
          ret = "A";
          break;
        case 1:
          ret = "B";
          break;
      }
      return ret;
    }).forEachOrdered(s -> {
      if (s != null) {
        MDC.put("callerId", s);
      }
      log.info("Hello Streamed {}", s == null ? "default" : s);
      MDC.clear();
    });
    */
  }

  @Test
  public void testDatabase() throws InterruptedException, IOException {
    Logger log = LoggerFactory.getLogger("db-logger");
    log.info("Database Test 1 {}", ZonedDateTime.now());
    log.info("Database Test 2 {}", ZonedDateTime.now());
    log.info("Database Test 3 {}", ZonedDateTime.now());
    log.info("Database Test 4 {}", ZonedDateTime.now());
    Thread.sleep(100l);
  }
}
