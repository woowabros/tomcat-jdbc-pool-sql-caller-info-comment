package in.woowa.tomcat.jdbc.pool.interceptor;

import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.AbstractCreateStatementInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * DB 쿼리 요청자에 관한 정보를 PreparedStatement SQL에 주석으로 남겨주는 Interceptor
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
public class SqlCallerInfoCommentInterceptor extends AbstractCreateStatementInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SqlCallerInfoCommentInterceptor.class);

    /**
     * Tomcat JDBC Pool Property 로 projectName을 지정하면 SQL 로그에 이 값이 남는다.
     */
    public static final String PROJECT_NAME_KEY = "projectName";

    /**
     * 숫자, 영문자, 밑줄, 공백만 허용된다.(SQL Injection 방어)
     */
    public static final Pattern VALIDATION_PATTERN = Pattern.compile("[\\w ]+");

    public static final String LOCALHOST_IPADDRESSES;
    public static final String UNKNOWN_LOCAL_IP = "unknown-local-ip";

    /**
     * PROJECT_NAME_PROPERTY 로 설정한 프로젝트명
     */
    private String projectName;

    static {
        LOCALHOST_IPADDRESSES = initializeLocalIpAddresses();
    }

    /**
     * Initialize Local IP Addresses
     */
    private static String initializeLocalIpAddresses() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            List<String> localIps = new ArrayList<>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        localIps.add(inetAddress.getHostAddress());
                    }
                }
            }

            return String.join(",", localIps);
        } catch (SocketException e) {
            log.error("Failed to initialize local IP addresses", e);
            return UNKNOWN_LOCAL_IP;
        }
    }

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
    public Object createStatement(Object proxy, Method method, Object[] args, Object statement, long time) {
        //  TODO - proxy statement if you want manipulate Statement.executeXXX methods
        return statement;
    }

    protected Object[] changeSql(Method method, Object[] args) {
        if (args == null) {
            return null;
        }

        final String methodName = method.getName();
        Object[] changedArgs = Arrays.copyOf(args, args.length);

        if (compare(PREPARE_STATEMENT, methodName) || compare(PREPARE_CALL, methodName)) {
            changedArgs[0] = commentSql((String) args[0]);
            log.debug("sql changed : {}", changedArgs[0]);
        } else {
            return args;
        }
        log.debug("sql not changed.");
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
            .append(" from ")
            .append(LOCALHOST_IPADDRESSES)
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
}
