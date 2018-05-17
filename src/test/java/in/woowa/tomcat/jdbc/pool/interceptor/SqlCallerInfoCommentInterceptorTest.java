package in.woowa.tomcat.jdbc.pool.interceptor;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class SqlCallerInfoCommentInterceptorTest {

    private SqlCallerInfoCommentInterceptor sqlCallerInfoCommentInterceptor;
    private Map<String, PoolProperties.InterceptorProperty> properties;

    @Before
    public void setUp() {
        sqlCallerInfoCommentInterceptor = new SqlCallerInfoCommentInterceptor();
        properties = new HashMap<>();
    }

    @Test
    public void localhostIpaddresses() {
        assertThat(SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES).isNotBlank();
    }

    @Test
    public void setProperty_projectName_null() {
        properties.clear();
        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName property must be set.");
        }
    }

    @Test
    public void setProperty_projectName_blank() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, ""));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName property must be set.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_asterisk() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123*"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123*' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_tab() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123\t"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123\t' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_dash() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123-"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123-' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_slash() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123/"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123/' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_hangul() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123한글"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123한글' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty_projectName_illegal_chars_newline() {
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, "ABC123abc123\n"));

        try {
            sqlCallerInfoCommentInterceptor.setProperties(properties);
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("projectName 'ABC123abc123\n' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    @Test
    public void setProperty() {
        String projectName = "abc123 ABC098_";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.getProjectName()).isEqualTo(projectName);
    }

    @Test
    public void changeSql_null_args() throws NoSuchMethodException {
        assertThat(sqlCallerInfoCommentInterceptor.changeSql(Connection.class.getMethod("createStatement"), null)).isNull();
    }


    @Test
    public void changeSql_just_createStatement() throws NoSuchMethodException {
        Object[] args = {1, 2};
        assertThat(sqlCallerInfoCommentInterceptor.changeSql(Connection.class.getMethod("createStatement", int.class, int.class), args)).isSameAs(args);
    }

    @Test
    public void changeSql_prepareStatement_commentSql() throws NoSuchMethodException {
        String projectName = "my_project";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.changeSql(Connection.class.getMethod("prepareStatement", String.class), new Object[]{"select 1"}))
            .containsExactly(" /* my_project from " + SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES + " */ select 1");
    }

    @Test
    public void changeSql_prepareCall_commentSql() throws NoSuchMethodException {
        String projectName = "my_project_sp";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.changeSql(Connection.class.getMethod("prepareCall", String.class), new Object[]{"SOME_SP"}))
            .containsExactly(" /* my_project_sp from " + SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES + " */ SOME_SP");
    }

    @Test
    public void commentSql() {
        String projectName = "my_project 007";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.commentSql("SELECT 1 FROM DUAL")).isEqualTo(" /* my_project 007 from " + SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES + " */ SELECT 1 FROM DUAL");
    }

    @Test
    public void changeExecuteSql_null_args() throws NoSuchMethodException {
        assertThat(sqlCallerInfoCommentInterceptor.changeExecuteSql(Statement.class.getMethod("executeQuery", String.class), null)).isNull();
    }


    @Test
    public void changeExecuteSql_just_execute() throws NoSuchMethodException {
        Object[] args = {"sql", 2};
        assertThat(sqlCallerInfoCommentInterceptor.changeExecuteSql(Statement.class.getMethod("execute", String.class, int.class), args)).isSameAs(args);
    }

    @Test
    public void changeExecuteSql_executeQuery_commentSql() throws NoSuchMethodException {
        String projectName = "my_project";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.changeExecuteSql(Statement.class.getMethod("executeQuery", String.class), new Object[]{"select 1"}))
            .containsExactly(" /* my_project from " + SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES + " */ select 1");
    }

    @Test
    public void changeExecuteSql_executeUpdate_commentSql() throws NoSuchMethodException {
        String projectName = "my_project_sp";
        properties.put(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, new PoolProperties.InterceptorProperty(SqlCallerInfoCommentInterceptor.PROJECT_NAME_KEY, projectName));
        sqlCallerInfoCommentInterceptor.setProperties(properties);

        assertThat(sqlCallerInfoCommentInterceptor.changeExecuteSql(Statement.class.getMethod("executeUpdate", String.class), new Object[]{"UPDATE ..."}))
            .containsExactly(" /* my_project_sp from " + SqlCallerInfoCommentInterceptor.LOCALHOST_IPADDRESSES + " */ UPDATE ...");
    }

}
