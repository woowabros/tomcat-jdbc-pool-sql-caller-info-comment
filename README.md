# tomcat-jdbc-pool-sql-caller-info-comment

> 이 리포는 우아한형제들 기술블로그의 [JDBC로 실행되는 SQL에 자동으로 프로젝트 정보 주석 남기기](https://techblog.woowahan.com/2584/) 아티클의 예제 코드입니다.

[Tomcat JDBC Connection Pool](https://tomcat.apache.org/tomcat-8.0-doc/jdbc-pool.html)의 `jdbcInterceptors`를 구현하여, 
JDBC PreparedStatement, Statement SQL 요청을 가로채어, 맨 앞에 호출자 관련 정보를 주석으로 넣고 DB로 전송한다.

예를들어 `SELECT 1 FROM DUAL` 이라는 쿼리가 있다면 실제로는 아래 쿼리가 DB로 전달된다. 

```sql
/* myproject_name */ SELECT 1 FROM DUAL
```

DB 관리자는 위 쿼리를 보고 호출자를 가늠할 수 있게 된다.

## 요구사항
* Java 8
* Tomcat JDBC Connection Pool 8

## 설치
[SqlCallerInfoCommentInterceptor](https://github.com/woowabros/tomcat-jdbc-pool-sql-caller-info-comment/blob/master/src/main/java/in/woowa/tomcat/jdbc/pool/interceptor/SqlCallerInfoCommentInterceptor.java) 소스를 복사하여
자신의 프로젝트에 넣는다.

## 설정
Tomcat JDBC Connection Pool 설정중에 `jdbcInterceptors` 프라퍼티를 다음과 같이 설정한다.


```java
import org.apache.tomcat.jdbc.pool.DataSource;

DataSource dataSource = new DataSource();
//... 기타 설정
dataSource.setJdbcInterceptors("in.woowa.tomcat.jdbc.pool.interceptor.SqlCallerInfoCommentInterceptor(projectName=woowahan)");
```
여기서 `woowahan`을 자신의 프로젝트 명으로 지정하면 된다.

**영문 대소문자, 숫자, 밑줄(_), 공백만 허용된다.**

실제 interceptor 에 대한 초기화는 최초의 DB Connection 요청이 발생하는 순간이다. 
`DataSource` 객체만 생성했다고 해서 interceptor가 초기화 되지는 않으므로 정확한 테스트는 실제 커넥션을 맺어봐야 한다. 
