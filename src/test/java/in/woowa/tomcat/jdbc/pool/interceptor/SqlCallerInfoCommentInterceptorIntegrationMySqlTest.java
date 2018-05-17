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
import static org.junit.Assume.assumeFalse;

/**
 * <code>-DdisableIntegrationTest=true</code> 이면 이 테스트를 실행하지 않는다.
 */
public class SqlCallerInfoCommentInterceptorIntegrationMySqlTest {

    private Logger log = LoggerFactory.getLogger(SqlCallerInfoCommentInterceptorIntegrationMySqlTest.class);

    private static DataSource dataSource;


    @BeforeClass
    public static void setUpClass() {
        assumeFalse(System.getProperty("disableIntegrationTest", "false").equals("true"));

        System.setProperty("org.slf4j.simplelogger.defaultlog", "debug");

        dataSource = new DataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test?allowMultiQueries=true&useUnicode=true&characterEncoding=utf8&logger=com.mysql.cj.core.log.Slf4JLogger&profileSQL=true");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        dataSource.setJdbcInterceptors("in.woowa.tomcat.jdbc.pool.interceptor.SqlCallerInfoCommentInterceptor(projectName=woowahan)");
        dataSource.setInitSQL("DROP TABLE IF EXISTS TESTUSER; CREATE TABLE TESTUSER (ID INT, NAME VARCHAR(50)); INSERT INTO TESTUSER (ID, NAME) VALUES(7, 'Baemin');");
        dataSource.setLogValidationErrors(true);
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


    @AfterClass
    public static void tearDownClass() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
