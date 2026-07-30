warning: in the working copy of 'backend/pom.xml', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'backend/src/main/resources/application.yml', LF will be replaced by CRLF the next time Git touches it
[1mdiff --git a/backend/pom.xml b/backend/pom.xml[m
[1mindex 0541ba2..90aae5f 100644[m
[1m--- a/backend/pom.xml[m
[1m+++ b/backend/pom.xml[m
[36m@@ -42,6 +42,10 @@[m
             <groupId>org.springframework.boot</groupId>[m
             <artifactId>spring-boot-starter-webmvc</artifactId>[m
         </dependency>[m
[32m+[m[32m        <dependency>[m
[32m+[m[32m            <groupId>org.springframework.boot</groupId>[m
[32m+[m[32m            <artifactId>spring-boot-starter-flyway</artifactId>[m
[32m+[m[32m        </dependency>[m
         <dependency>[m
             <groupId>org.mybatis.spring.boot</groupId>[m
             <artifactId>mybatis-spring-boot-starter</artifactId>[m
[36m@@ -53,6 +57,10 @@[m
             <artifactId>mysql-connector-j</artifactId>[m
             <scope>runtime</scope>[m
         </dependency>[m
[32m+[m[32m        <dependency>[m
[32m+[m[32m            <groupId>org.flywaydb</groupId>[m
[32m+[m[32m            <artifactId>flyway-mysql</artifactId>[m
[32m+[m[32m        </dependency>[m
         <dependency>[m
             <groupId>io.jsonwebtoken</groupId>[m
             <artifactId>jjwt-api</artifactId>[m
[1mdiff --git a/backend/src/main/resources/application.yml b/backend/src/main/resources/application.yml[m
[1mindex f51eab2..eee982e 100644[m
[1m--- a/backend/src/main/resources/application.yml[m
[1m+++ b/backend/src/main/resources/application.yml[m
[36m@@ -11,6 +11,13 @@[m [mspring:[m
     password: ${DB_PASSWORD:}[m
     driver-class-name: com.mysql.cj.jdbc.Driver[m
 [m
[32m+[m[32m  flyway:[m
[32m+[m[32m    enabled: ${FLYWAY_ENABLED:true}[m
[32m+[m[32m    locations: classpath:db/migration[m
[32m+[m[32m    validate-on-migrate: true[m
[32m+[m[32m    clean-disabled: true[m
[32m+[m[32m    baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}[m
[32m+[m[32m    baseline-version: 1[m
 [m
 mybatis:[m
   mapper-locations: classpath:mapper/*.xml[m
