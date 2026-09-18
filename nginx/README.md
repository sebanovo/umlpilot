# UmlPilot - nginx de borde

nginx que actúa como **punto de entrada único** del despliegue (HTTP, sin dominio):

- `/` → `umlpilot-web:80` (SPA de React)
- `/api/` → `umlpilot-api:8080` (API REST)
- `/ws` → `umlpilot-api:8080` (WebSocket STOMP de colaboración)

## Estructura

- `Dockerfile`: imagen `nginx:stable-alpine` con `default.conf`.
- `default.conf`: virtual host HTTP. Incluye (comentados) los bloques para HTTPS.
- `HTTPS.md`: notas para cuando haya dominio y certificados.

## Despliegue

No se levanta por separado: lo orquesta `docker-compose.prod.yml` como servicio
`umlpilot-nginx`.

```bash
# Desde la raíz del repositorio
docker compose -f docker-compose.prod.yml up -d --build
# La app queda en http://<host>:${FRONTEND_PORT} (por defecto el del .env)
```

## Imágenes (opcional, para publicar en un registro)

```bash
docker build -t <usuario>/umlpilot-api:1.0.0 ./umlpilot_api -f ./umlpilot_api/Dockerfile.prod
docker build -t <usuario>/umlpilot-web:1.0.0 ./umlpilot_web -f ./umlpilot_web/Dockerfile.prod
docker build -t <usuario>/umlpilot-nginx:1.0.0 ./nginx -f ./nginx/Dockerfile

docker push <usuario>/umlpilot-api:1.0.0
docker push <usuario>/umlpilot-web:1.0.0
docker push <usuario>/umlpilot-nginx:1.0.0
```
