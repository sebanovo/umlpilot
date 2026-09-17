spring:
  application:
    name: $model.applicationName$

  # Perfil activo (vacío = H2 por defecto; usa 'postgres' para PostgreSQL)
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:}

  # Base de datos por defecto: H2 en archivo, con la ruta raíz /data
  datasource:
    url: jdbc:h2:file:${H2_DATA_PATH:./data}/generated_db;AUTO_SERVER=TRUE
    username: \${H2_USER:sa}
    password: \${H2_PASSWORD:}
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        # Cita todos los identificadores para admitir nombres de columna que son
        # palabras reservadas (por ejemplo 'value') tanto en H2 como en PostgreSQL
        globally_quoted_identifiers: true

server:
  port: \${SERVER_PORT:8081}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
