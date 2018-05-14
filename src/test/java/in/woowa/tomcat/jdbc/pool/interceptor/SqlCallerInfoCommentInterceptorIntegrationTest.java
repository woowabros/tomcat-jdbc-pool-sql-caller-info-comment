package in.woowa.tomcat.jdbc.pool.interceptor;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlCallerInfoCommentInterceptorIntegrationTest {

    private Logger log = LoggerFactory.getLogger(SqlCallerInfoCommentInterceptorIntegrationTest.class);

    private static DataSource dataSource;


    @BeforeClass
    public static void setUpClass() {
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

                        log.info("id : {} -> {}", id, name);

                        assertThat(id).isEqualTo(7);
                        assertThat(name).isEqualTo("Baemin");
                    }
                }
            }
        }
    }

    @Test
    public void statementExecuteQuery() {

    }

    @AfterClass
    public static void tearDownClass() {
        dataSource.close();
    }
}
