# Build stage (Vite 8 requires Node 20.19+ / 22.12+)
FROM node:22-alpine AS build
WORKDIR /app
# Optional outbound proxy for restricted networks (Docker Desktop: http://http.docker.internal:3128).
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ARG NO_PROXY=""
ENV HTTP_PROXY=${HTTP_PROXY} HTTPS_PROXY=${HTTPS_PROXY} NO_PROXY=${NO_PROXY}
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Baked into the bundle at build time. The default "/api" is proxied to the backend by nginx.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Run stage
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
