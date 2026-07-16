FROM node:22.18.0-alpine
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY src ./src

EXPOSE 8180

CMD ["node", "src/server.js"]
