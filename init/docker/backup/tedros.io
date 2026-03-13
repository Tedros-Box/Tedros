# ----------------------------------------------------
# 1. Bloco HTTPS (Porta 443) - tedros.io
# ----------------------------------------------------
server {
    server_name tedros.io www.tedros.io; # Atende o domínio principal com e sem WWW

    listen 443 ssl http2;

    location / {
        # Encaminha para o site local no path específico
        # O '/' ao final é importante para o mapeamento correto
        proxy_pass                http://127.0.0.1:8081/tedros-io/;
        
        proxy_set_header          Host $host;
        proxy_set_header          X-Real-IP $remote_addr;
        proxy_set_header          X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header          X-Forwarded-Proto $scheme;

        proxy_buffering           off;
    }

    # As linhas abaixo serão preenchidas/ajustadas pelo Certbot
    # ssl_certificate ...
    # ssl_certificate_key ...

    ssl_certificate /etc/letsencrypt/live/tedros.io/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/tedros.io/privkey.pem; # managed by Certbot

}

# ----------------------------------------------------
# 2. Bloco HTTP (Porta 80) - Redirecionamento
# ----------------------------------------------------
server {
    if ($host = www.tedros.io) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    if ($host = tedros.io) {
        return 301 https://$host$request_uri;
    } # managed by Certbot


    listen 80;
    server_name tedros.io www.tedros.io;

    # Redireciona para HTTPS
    return 301 https://$host$request_uri;




}
