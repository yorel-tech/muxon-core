FROM node:20-alpine AS deps
WORKDIR /app

# Copy package files from web directory
COPY web/package.json web/package-lock.json ./

# Install dependencies
RUN npm ci

FROM node:20-alpine AS build
WORKDIR /app

# Copy dependencies
COPY --from=deps /app/node_modules ./node_modules

# Copy source code
COPY web .

# Build the application
RUN npm run build

FROM node:20-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production

# Copy the built application
COPY --from=build /app/package.json /app/package-lock.json ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/.next ./.next
# COPY --from=build /app/public ./public

EXPOSE 3000

# Start the application
CMD ["npm", "start"]
