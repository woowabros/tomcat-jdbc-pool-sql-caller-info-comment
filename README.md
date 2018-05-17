# tomcat-jdbc-pool-sql-caller-info-comment

[Tomcat JDBC Connection Pool](https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html)의 `jdbcInterceptors`를 구현하여, 
JDBC PreparedStatement SQL 요청을 가로채어, 맨 앞에 호출자 관련 정보와 IP Address들을 주석으로 넣고 DB로 전송한다.

예를들어 `SELECT 1 FROM DUAL` 이라는 쿼리가 있다면 실제로는 아래 쿼리가 DB로 전달된다. 

```sql
/* myproject_name from 192.168.1.1,... */ SELECT 1 FROM DUAL 
```

DB 관리자는 위 쿼리를 보고 호출자를 가늠할 수 있게 된다.

## 요구사항
* Java 8
* Tomcat JDBC Connection Pool 8

## 설치
[SqlCallerInfoCommentInterceptor](https://github.com/woowabros/tomcat-jdbc-pool-sql-caller-info-comment/blob/master/src/main/java/in/woowa/tomcat/jdbc/pool/interceptor/SqlCallerInfoCommentInterceptor.java) 소스를 복사하여
자신의 프로젝트에 넣는다.

혹은 gradle에서 의존성 지정

```
compile 'in.woowa:tomcat-jdbc-pool-sql-caller-info-comment:0.5'
```

## 설정
Tomcat JDBC Connection Pool 설정중에 `jdbcInterceptors` 프라퍼티를 다음과 같이 설정한다.


```java
import org.apache.tomcat.jdbc.pool.DataSource;

DataSource dataSource = new DataSource();
//... 기타 설정
dataSource.setJdbcInterceptors("in.woowa.tomcat.jdbc.pool.interceptor.SqlCallerInfoCommentInterceptor(projectName=woowahan)");
```
여기서 `wowahan`을 자신의 프로젝트 명으로 지정하면 된다.

**영문 대소문자, 숫자, 밑줄(_), 공백만 허용된다.**

