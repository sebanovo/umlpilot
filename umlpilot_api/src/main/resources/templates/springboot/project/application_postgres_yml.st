# Perfil secundario: PostgreSQL
# Actívalo con SPRING_PROFILES_ACTIVE=postgres (o --spring.profiles.active=postgres)
spring:
  datasource:
    url: jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:generated_db}
    username: \${DB_USER:postgres}
    password: \${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    database: POSTGRESQL
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
