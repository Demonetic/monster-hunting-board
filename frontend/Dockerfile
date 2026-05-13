FROM node:20-alpine AS builder
WORKDIR /app

COPY . .
RUN npm install && npm run build

FROM nginx:alpine
ARG NGINX_CONF=nginx.conf
COPY --from=builder /app/dist /usr/share/nginx/html
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf

EXPOSE 80
