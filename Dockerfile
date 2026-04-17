FROM node:22-alpine

WORKDIR /app

# Copiar todo el repo
COPY . .

# Entrar en Front y construir
WORKDIR /app/Front

RUN npm install -g pnpm && \
    pnpm install --frozen-lockfile && \
    pnpm build

# Servir la app construida
EXPOSE 5173

CMD ["pnpm", "dlx", "serve", "-s", "dist", "-l", "5173"]
