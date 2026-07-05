# CipherLink

**A real-time, end-to-end encrypted messaging platform** — built with a Spring Boot backend and a Next.js/TypeScript frontend, supporting 1:1 and group messaging, voice/video calling, disappearing stories, and live location sharing.

CipherLink is designed around a simple principle: **the server should never see plaintext**. Messages are encrypted end-to-end using a Signal-protocol-inspired key exchange, so the backend only ever stores and relays ciphertext.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Real-Time Communication](#real-time-communication)
- [Security](#security)
- [API Documentation](#api-documentation)

---

## Overview

CipherLink is a full-stack messaging application that mirrors the core experience of apps like WhatsApp and Signal: instant message delivery, delivery/read receipts, typing indicators, voice/video calls, and disappearing stories — all backed by end-to-end encryption.

The system is split into two independently deployable services:

| Service | Description |
|---|---|
| **`cipher_backend`** | Java / Spring Boot REST + WebSocket API |
| **`d` (frontend)** | Next.js / TypeScript client application |

---

## Features

- **1:1 and group messaging** with edit, delete, reply, and emoji reactions
- **Delivery & read receipts** (sent → delivered → seen) and typing indicators
- **End-to-end encryption** using a Signal-protocol-inspired pre-key/session exchange — the server only relays encrypted payloads
- **Voice & video calling** via WebRTC, with signaling relayed over the existing WebSocket connection
- **Disappearing stories** (24-hour expiring media posts) with view tracking
- **Live location sharing** within a conversation
- **Push notifications** for offline recipients via Firebase Cloud Messaging
- **JWT authentication** with refresh-token rotation and rate limiting
- **Multi-device support** via per-device key/session management

---

## Architecture

```
┌──────────────────────┐        REST (auth, contacts,        ┌───────────────────────┐
│                       │        media, history)              │                       │
│   Next.js Frontend    │ ───────────────────────────────────▶│   Spring Boot API     │
│   (TypeScript, React) │◀─────────────────────────────────── │                       │
│                       │                                      │                       │
│                       │        STOMP over WebSocket          │                       │
│                       │◀════════════════════════════════════▶│   (messages, typing,  │
│                       │        (SockJS fallback)             │   presence, calls)    │
└──────────────────────┘                                       └──────────┬────────────┘
                                                                            │
                                                                    PostgreSQL
                                                              (encrypted payloads only)
```

- The client authenticates via Firebase phone OTP, then exchanges that for the app's own **JWT access/refresh token pair**.
- **REST APIs** handle everything that isn't latency-sensitive: profile, contacts, media, message history, groups, stories, and pre-key/session setup.
- A single **persistent STOMP-over-WebSocket connection** handles everything real-time: new messages, reactions, typing indicators, presence, and call signaling (offer/answer/ICE candidates).
- **PostgreSQL** persists conversations and messages, but message bodies are encrypted client-side before they ever reach the server.

---

## Tech Stack

### Backend — `cipher_backend`

| Category | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3 |
| Real-time | Spring WebSocket + STOMP (SockJS fallback), custom JWT-authenticated `ChannelInterceptor` |
| Security | Spring Security, JWT (`jjwt`), refresh-token rotation, rate limiting (`bucket4j`) |
| Persistence | Spring Data JPA, PostgreSQL |
| Encryption | Custom `CryptoService`, `PreKeyService`, `SessionService` — Signal-protocol-inspired pre-key/session exchange |
| Notifications & Auth | Firebase Admin SDK (phone OTP, push notifications) |
| Docs | springdoc-openapi (Swagger UI) |

**Key backend modules**

```
com.cipherlink
├── config/       # Security, WebSocket, CORS configuration
├── controller/   # REST controllers (Auth, User, Message, Group, Story, Media, PreKey, Session)
├── dto/          # Request/response payloads
├── hub/          # WebSocket message hub (ChatHub, WebSocketConfig, StompPrincipal)
├── model/        # JPA entities (User, Conversation, Message, Group, Story, Session, PreKey, ...)
├── repository/   # Spring Data JPA repositories
├── security/     # JwtAuthFilter, security headers, request logging
└── service/      # Business logic (crypto, sessions, media, push notifications, cleanup jobs)
```

### Frontend — `d`

| Category | Technology |
|---|---|
| Framework | Next.js 16 (App Router, Turbopack), React 19, TypeScript |
| Real-time client | `@stomp/stompjs` + `sockjs-client` — custom `CipherLinkWS` client with automatic reconnect, proactive JWT refresh, and an offline outbox queue |
| State management | Zustand (auth, chat, calls stores) |
| Styling / UI | Tailwind CSS v4, Radix UI primitives, `lucide-react`, Framer Motion |
| Forms | React Hook Form + Zod |
| Calling | WebRTC (`RTCPeerConnection`, `getUserMedia`) |
| Auth | Firebase phone-OTP sign-in |

---

## Project Structure

```
cipherlink/
├── cipher_backend/            # Spring Boot backend
│   ├── src/main/java/com/cipherlink/
│   └── src/main/resources/application.properties
└── d/                          # Next.js frontend
    ├── src/app/                # App Router pages (auth, chat, calls, stories, profile)
    ├── src/components/         # UI components (providers, calls overlay, etc.)
    ├── src/hooks/               # useWebSocket, etc.
    ├── src/lib/                 # api.ts (REST client), websocket.ts (STOMP client)
    ├── src/store/                # Zustand stores (auth, chat, calls)
    └── src/types/                 # Shared TypeScript types
```

---

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 14+
- A Firebase project (for phone OTP auth and push notifications)

### Backend

```bash
cd cipher_backend
# configure application.properties or a .env file (see Environment Variables below)
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` by default.

### Frontend

```bash
cd d
cp .env.example .env.local   # fill in the values below
npm install
npm run dev
```

The app starts on `http://localhost:3000` by default.

---

## Environment Variables

### Backend (`application.properties` / `.env`)

| Variable | Description |
|---|---|
| `PORT` | Server port (default `8080`) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins |
| `JWT_SECRET` | Secret used to sign access/refresh tokens |
| Database connection | Standard Spring `spring.datasource.*` properties for PostgreSQL |
| Firebase credentials | Service account credentials for Firebase Admin SDK |

### Frontend (`.env.local`)

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_URL` | Base URL of the backend REST API |
| `NEXT_PUBLIC_WS_URL` | WebSocket (SockJS) endpoint, e.g. `http://localhost:8080/ws` |
| `NEXT_PUBLIC_FIREBASE_*` | Firebase client config (API key, auth domain, project ID, app ID) |

---

## Real-Time Communication

CipherLink uses a single **STOMP-over-WebSocket** connection per client for all real-time features:

| Destination | Purpose |
|---|---|
| `/app/chat.sendMessage` | Send a 1:1 message |
| `/app/chat.react` / `.editMessage` / `.deleteMessage` | Message actions |
| `/app/chat.typingStarted` / `.typingStopped` | Typing indicators |
| `/app/call.initiate` / `.answer` / `.ice` / `.end` | WebRTC call signaling |
| `/user/queue/messages` | Per-user inbound message/ack channel |
| `/user/queue/calls` | Per-user inbound call signaling channel |
| `/topic/presence` | Online/offline presence broadcast |

The frontend's WebSocket client (`lib/websocket.ts`) is built to be resilient:
- Automatically refreshes the JWT before it expires (chat traffic never touches REST, so this can't rely on a 401-triggered refresh).
- Queues any outgoing action (message, reaction, typing event) if the socket is momentarily disconnected, and flushes the queue automatically on reconnect — nothing is silently dropped.
- Uses optimistic UI updates: a sent message appears instantly on the sender's screen and is reconciled with the server's confirmation once it arrives, rather than waiting on the round trip.

---

## Security

- **JWT authentication** for both REST and WebSocket (STOMP `CONNECT` handshakes are authenticated via a custom `ChannelInterceptor`).
- **Refresh-token rotation** to limit the blast radius of a leaked token.
- **Rate limiting** on sensitive endpoints via `bucket4j`.
- **End-to-end encryption**: message payloads are encrypted client-side using a Signal-protocol-inspired pre-key/session exchange (`PreKeyService`, `SessionService`, `CryptoService`) — the server never has access to plaintext message content.
- **Per-device sessions**, allowing a user to be logged in on multiple devices with independent encrypted sessions.

---

## API Documentation

Once the backend is running, interactive API docs (Swagger UI) are available at:

```
http://localhost:8080/swagger-ui.html
```

---

## License

This project is for educational/portfolio purposes.
