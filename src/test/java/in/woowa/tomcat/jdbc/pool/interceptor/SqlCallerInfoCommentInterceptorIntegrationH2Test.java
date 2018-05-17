package in.woowa.tomcat.jdbc.pool.interceptor;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlCallerInfoCommentInterceptorIntegrationH2Test {

    private Logger log = LoggerFactory.getLogger(SqlCallerInfoCommentInterceptorIntegrationH2Test.class);

    private static DataSource dataSource;


    @BeforeClass
    public static void setUpClass() {
        System.setProperty("org.slf4j.simplelogger.defaultlog", "debug");

        dataSource = new DataSource();
        dataSource.setUrl("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4;TRACE_LEVEL_SYSTEM_OUT=3");
        dataSource.setJdbcInterceptors("in.woowa.tomcat.jdbc.pool.interceptor.SqlCallerInfoCommentInterceptor(projectName=woowahan)");
        dataSource.setInitSQL("DROP TABLE TESTUSER IF EXISTS; CREATE TABLE TESTUSER (ID INT, NAME VARCHAR(50)); INSERT INTO TESTUSER (ID, NAME) VALUES(7, 'Baemin');");
    }

    @Test
    public void preparedStatement() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement("/* base comment */ SELECT * FROM TESTUSER")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String name = rs.getString(2);

                        log.debug("id : {} -> {}", id, name);

                        assertThat(id).isEqualTo(7);
                        assertThat(name).isEqualTo("Baemin");
                    }
                }
            }
        }
    }

    @Test
    public void getMetaData() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            DatabaseMetaData metaData = con.getMetaData();
            assertThat(metaData.getDatabaseMajorVersion()).isEqualTo(1);
        }
    }

    @Test
    public void createStatement() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            try (Statement stmt = con.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("/* just statement */ SELECT * FROM TESTUSER")) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String name = rs.getString(2);

                        log.debug("statement id : {} -> {}", id, name);

                        assertThat(id).isEqualTo(7);
                        assertThat(name).isEqualTo("Baemin");

                    }
                }

                int updated = stmt.executeUpdate("UPDATE TESTUSER SET NAME='배민' WHERE id=7");
                assertThat(updated).isEqualTo(1);
            }
        }
    }

    @AfterClass
    public static void tearDownClass() {
        dataSource.close();
    }
}
