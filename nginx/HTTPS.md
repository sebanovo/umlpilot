# 📁 Documentación: Configuración de HTTPS con Let's Encrypt
---

## 1️⃣ **Verificar Security Group AWS**

En AWS EC2 Console, tu instancia debe tener abiertos estos puertos:

| Puerto | Tipo | Uso |
|--------|------|-----|
| 22 | SSH | Acceso seguro |
| 80 | HTTP | Let's Encrypt + redirección |
| 443 | HTTPS | Tráfico seguro |

> ⚠️ **Importante**: Estos puertos son necesarios para que Let's Encrypt pueda validar tu dominio.

---

## 2️⃣ **Verificar que DNS ya funciona**

```bash
curl http://sebaces.fyi
```

Debe abrir tu aplicación. Si no abre:
- Espera propagación DNS (puede tardar minutos/horas)
- Revisa firewall de EC2
- Revisa que los contenedores Docker estén corriendo

---

## 3️⃣ **Conectarse a EC2**

```bash
ssh -i tu-clave.pem ubuntu@3.144.192.20
```

Reemplaza `tu-clave.pem` con la ruta a tu clave SSH y la IP con la de tu instancia.

---

## 4️⃣ **Instalar Certbot**

```bash
sudo apt update
sudo apt install certbot -y
```

Certbot es la herramienta que nos permite obtener certificados SSL gratuitos de Let's Encrypt.

---

## 5️⃣ **Detener Nginx temporalmente**

Certbot necesita usar el puerto 80 directamente para validar el dominio.

```bash
# En la carpeta del proyecto
docker compose down
```

> ⚠️ **Importante**: El puerto 80 debe estar libre.

---

## 6️⃣ **Generar certificado SSL real**

```bash
sudo certbot certonly --standalone -d sebaces.fyi
```

Te pedirá:
- Email (para notificaciones de renovación)
- Aceptar términos de servicio
- (Opcional) Compartir email con EFF

**Certificados generados en:**
```
/etc/letsencrypt/live/sebaces.fyi/
```

**Archivos importantes:**
- `fullchain.pem` - Certificado completo
- `privkey.pem` - Clave privada

---

## 7️⃣ **Modificar `docker-compose.yml`**

### Antes:
```yaml
volumes:
  - ./nginx/ssl:/etc/nginx/ssl:ro
```

### Después:
```yaml
volumes:
  - /etc/letsencrypt:/etc/letsencrypt:ro
```

---

## 8️⃣ **Configurar Nginx (`default.conf`)**

### Archivo completo reemplazando el existente:

```nginx
# HTTP -> Redirección a HTTPS
server {
    listen 80;
    server_name sebaces.fyi;

    return 301 https://$host$request_uri;
}

# HTTPS real
server {
    listen 443 ssl;
    http2 on;

    server_name sebaces.fyi;

    # Certificados SSL
    ssl_certificate /etc/letsencrypt/live/sebaces.fyi/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/sebaces.fyi/privkey.pem;

    # Seguridad SSL (recomendado)
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    client_max_body_size 20M;

    # Angular Web
    location / {
        proxy_pass http://serv-finance-web:80;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API Backend
    location /api/ {
        proxy_pass http://serv-finance-api:8080;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Flutter Web
    location /flutter/ {
        proxy_pass http://serv-finance-flutter-web:80/;

        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 9️⃣ **Levantar contenedores nuevamente**

```bash
docker compose up -d
```

---

## 🔟 **Probar HTTPS**

Abre en tu navegador:

```
https://sebaces.fyi
```

Verifica:
- 🔒 Candado verde (sitio seguro)
- ✅ Certificado válido
- ✅ Sin advertencias de seguridad

---

## 1️⃣1️⃣ **Verificar renovación automática**

Los certificados Let's Encrypt duran **90 días**. La renovación automática es crítica.

### Prueba de renovación (simulacro):

```bash
sudo certbot renew --dry-run
```

Si ves `** DRY RUN: simulating 'certbot renew'... **` todo está correcto.

### Verificar timers automáticos:

```bash
systemctl list-timers | grep certbot
```

Deberías ver algo como `certbot.timer`.

### Renovación manual (si es necesario):

```bash
sudo certbot renew
```