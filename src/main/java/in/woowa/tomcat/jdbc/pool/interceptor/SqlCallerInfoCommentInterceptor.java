package in.woowa.tomcat.jdbc.pool.interceptor;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.StatementDecoratorInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DB 쿼리 요청자에 관한 정보를 Statement SQL에 주석으로 삽입해 주는 Interceptor - DBA 등이 이 주석을 보고 호출한 프로젝트를 판별할 수 있게 해준다.
 *
 * <h3>SQL 에 남기는 주석 내용</h3>
 * <ul>
 * <li>호출자 IP</li>
 * <li>호출자 애플리케이션 이름 : projectName 키로 DataSource property 에 지정해준다. 숫자, 영문자, 밑줄, 공백만 허용된다.(SQL Injection 방어)</li>
 * </ul>
 *
 * <h3>설정</h3>
 * <a href="https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html">tomcat jdbc connection pool</a>을 사용하여 SQL을 조작한다.
 * <pre>dataSource.setJdbcInterceptors("in.woowa.tomcat.jdbc.pool.interceptor.SqlCallerInfoCommentInterceptor(projectName=[YourProjectName])");</pre>
 *
 * @see <a href="https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html">tomcat jdbc connection pool</a>
 */
public class SqlCallerInfoCommentInterceptor extends StatementDecoratorInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlCallerInfoCommentInterceptor.class);

    /**
     * Tomcat JDBC Pool Property 로 projectName을 지정하면 SQL 로그에 이 값이 남는다.
     */
    public static final String PROJECT_NAME_KEY = "projectName";

    /**
     * 숫자, 영문자, 밑줄, 공백만 허용된다.(SQL Injection 방어)
     */
    public static final Pattern VALIDATION_PATTERN = Pattern.compile("[\\w ]+");

    /**
     * PROJECT_NAME_PROPERTY 로 설정한 프로젝트명
     */
    private String projectName;

    public String getProjectName() {
        return projectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (compare(CLOSE_VAL, method)) {
            return super.invoke(proxy, method, args);
        }

        boolean process = false;
        process = isStatement(method, process);
        if (!process) {
            return super.invoke(proxy, method, args);
        }

        Object[] changedArgs = changeSql(method, args);
        return super.invoke(proxy, method, changedArgs);
    }

    @Override
    protected Object createDecorator(Object proxy, Method method, Object[] args, Object statement, Constructor<?> constructor, String sql) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Object result = null;
        SqlChangeStatementProxy<Statement> statementProxy =
            new SqlChangeStatementProxy<>((Statement) statement, sql);
        result = constructor.newInstance(new Object[]{statementProxy});
        statementProxy.setActualProxy(result);
        statementProxy.setConnection(proxy);
        statementProxy.setConstructor(constructor);
        return result;
    }

    public boolean isExecuteUpdate(Method method) {
        return isExecuteUpdate(method.getName());
    }

    private boolean isExecuteUpdate(String name) {
        return EXECUTE_UPDATE.equals(name);
    }

    /**
     * Connection.prepareStatement 를 위한 Sql 변경
     */
    protected Object[] changeSql(Method method, Object[] args) {
        if (args == null) {
            log.debug("sql not changed {}", method);

            return null;
        }

        final String methodName = method.getName();
        Object[] changedArgs = Arrays.copyOf(args, args.length);

        // PREPARE_CALL does not support comment prefix
        if (compare(PREPARE_STATEMENT, methodName)) {
            changedArgs[0] = commentSql((String) args[0]);
            log.debug("sql changed : {}, {}", method, changedArgs[0]);
        } else {
            log.debug("sql not changed {}", method);
            return args;
        }
        return changedArgs;
    }

    /**
     * Statement.executeQuery, executeUpdate 를 위한 Sql 변경
     */
    protected Object[] changeExecuteSql(Method method, Object[] args) {
        if (args == null) {
            log.debug("sql not changed {}", method);
            return args;
        }

        if (!isExecuteQuery(method) && !isExecuteUpdate(method)) {
            log.debug("sql not changed {}", method);
            return args;
        }

        Object[] changedArgs = Arrays.copyOf(args, args.length);
        changedArgs[0] = commentSql((String) args[0]);
        log.debug("sql changed : {}, {}", method, changedArgs[0]);
        return changedArgs;
    }

    @Override
    public void closeInvoked() {
        // no op
    }

    /**
     * 실질적인 주석 넣기를 수행한다.
     *
     * @param sql 원본 SQL
     * @return 주석이 추가된 SQL
     */
    protected String commentSql(String sql) {
        StringBuilder builder = new StringBuilder();
        builder.append(" /* ")
            .append(projectName)
            .append(" */ ")
            .append(sql);
        return builder.toString();
    }

    @Override
    public void setProperties(Map<String, PoolProperties.InterceptorProperty> properties) {
        super.setProperties(properties);

        PoolProperties.InterceptorProperty projectNameProperty = properties.get(PROJECT_NAME_KEY);

        if (projectNameProperty == null || projectNameProperty.getValue() == null || projectNameProperty.getValue().trim().isEmpty()) {
            throw new IllegalArgumentException("projectName property must be set.");
        }
        projectName = projectNameProperty.getValue();
        log.info("projectName : {}", projectName);

        if (!VALIDATION_PATTERN.matcher(projectName).matches()) {
            throw new IllegalArgumentException("projectName '" + projectName + "' contains illegal chars. projectName must contain only alpha numerics, spaces and underscores.");
        }
    }

    protected class SqlChangeStatementProxy<T extends java.sql.Statement> extends StatementProxy<T> {

        public SqlChangeStatementProxy(T delegate, String sql) {
            super(delegate, sql);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object[] changedArgs = changeExecuteSql(method, args);
            return super.invoke(proxy, method, changedArgs);
        }

    }
}
