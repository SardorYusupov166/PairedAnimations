package dev.sardoryusupov.pairedanimations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Optional private WebSocket relay. No Minecraft-server plugin is required.
 * In production use only a trusted WSS relay; this prototype cannot prove
 * that a remote WebSocket registration actually belongs to a Mojang account.
 */
public final class PairRelayClient {
    private static final long RECONNECT_MS = 5_000L;
    private static final long MAX_INVITE_DISTANCE_SQUARED = 36L;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6)).build();
    private final String endpoint = loadEndpoint();
    private WebSocket socket;
    private UUID localId;
    private String serverScope;
    private boolean connecting;
    private boolean ready;
    private long nextConnectAt;
    private long generation;
    private long helloSentMillis;
    private long serverClockOffsetMillis;
    private Invitation incoming;
    private UUID targetPlayer;

    public boolean isEnabled() {
        return endpoint != null;
    }

    public boolean isReady() {
        return ready && socket != null;
    }

    public void tick(Minecraft client) {
        if (!isEnabled()) return;
        if (client.player == null || client.level == null || client.getCurrentServer() == null) {
            if (socket != null || connecting) disconnect();
            return;
        }
        String scope = client.getCurrentServer().ip.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9.:_-]", "_");
        UUID id = client.player.getUUID();
        if (!scope.equals(serverScope) || !id.equals(localId)) {
            disconnect();
            serverScope = scope;
            localId = id;
        }
        if (incoming != null && incoming.expiresAtMillis < System.currentTimeMillis()) {
            incoming = null;
        }
        if (!ready && socket == null && !connecting && System.currentTimeMillis() > nextConnectAt) {
            connect(client);
        }
    }

    private void connect(Minecraft client) {
        final long attempt = ++generation;
        connecting = true;
        try {
            httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .buildAsync(URI.create(endpoint), new Receiver(client, attempt))
                .whenComplete((webSocket, failure) -> client.execute(() -> {
                    if (generation != attempt) {
                        if (webSocket != null) webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "stale");
                        return;
                    }
                    connecting = false;
                    if (failure != null) {
                        nextConnectAt = System.currentTimeMillis() + RECONNECT_MS;
                    } else {
                        socket = webSocket;
                    }
                }));
        } catch (Exception invalidUri) {
            connecting = false;
            nextConnectAt = System.currentTimeMillis() + 60_000L;
            message(client, "Некорректный адрес relay-сервера.");
        }
    }

    private void disconnect() {
        generation++;
        WebSocket old = socket;
        socket = null;
        ready = false;
        connecting = false;
        incoming = null;
        targetPlayer = null;
        nextConnectAt = System.currentTimeMillis() + RECONNECT_MS;
        if (old != null) old.sendClose(WebSocket.NORMAL_CLOSURE, "leaving");
    }

    public void invite(Minecraft client, Player partner) {
        if (!isReady()) {
            message(client, "Relay не подключён. Используется локальный предпросмотр.");
            PairedAnimationsClient.CINEMATIC.start(client, partner);
            return;
        }
        targetPlayer = partner.getUUID();
        JsonObject json = new JsonObject();
        json.addProperty("type", "invite");
        json.addProperty("to", targetPlayer.toString());
        json.addProperty("dance", "tango");
        send(json);
    }

    public void accept(Minecraft client) {
        if (incoming == null || !isReady()) {
            message(client, "Нет активного приглашения.");
            return;
        }
        Player partner = findPlayer(client, incoming.from);
        if (partner == null || client.player == null
            || client.player.distanceToSqr(partner) > MAX_INVITE_DISTANCE_SQUARED) {
            decline(client);
            message(client, "Партнёр слишком далеко или покинул игру.");
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "accept");
        json.addProperty("inviteId", incoming.id);
        send(json);
        incoming = null;
        message(client, "Приглашение принято. Подготовка к танцу...");
    }

    public void decline(Minecraft client) {
        if (incoming == null) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "decline");
        json.addProperty("inviteId", incoming.id);
        send(json);
        incoming = null;
        message(client, "Приглашение отклонено.");
    }

    public void cancel() {
        if (!isReady()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "cancel");
        send(json);
        targetPlayer = null;
    }

    private void send(JsonObject json) {
        if (socket != null) socket.sendText(json.toString(), true);
    }

    private static Player findPlayer(Minecraft client, UUID uuid) {
        if (client.level == null) return null;
        for (Player player : client.level.players()) {
            if (player.getUUID().equals(uuid) && player.isAlive() && !player.isSpectator()) return player;
        }
        return null;
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) client.player.sendSystemMessage(Component.literal("[PairedAnimations] " + text));
    }

    private static String loadEndpoint() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("pairedanimations-relay.txt");
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path,
                "# PairedAnimations relay endpoint. One URL per file. Restart Minecraft to apply.\n" +
                "# Example local test: ws://127.0.0.1:8765\n" +
                "# Example hosted TLS relay: wss://relay.example.org\n",
                StandardCharsets.UTF_8);
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                URI uri = URI.create(trimmed);
                if ("wss".equalsIgnoreCase(uri.getScheme())) return trimmed;
                if ("ws".equalsIgnoreCase(uri.getScheme())
                    && ("127.0.0.1".equals(uri.getHost()) || "localhost".equalsIgnoreCase(uri.getHost()))) return trimmed;
                return null;
            }
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private void handle(Minecraft client, String json) {
        if (client.player == null || client.level == null) return;
        final JsonObject data;
        try {
            data = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception malformed) {
            return;
        }
        if (!data.has("type")) return;
        try {
            switch (data.get("type").getAsString()) {
                case "ready" -> {
                    ready = true;
                    long now = System.currentTimeMillis();
                    serverClockOffsetMillis = data.get("serverTime").getAsLong()
                        - ((helloSentMillis + now) / 2);
                    message(client, "Подключение к relay установлено. K — пригласить, J — принять, N — отклонить.");
                }
                case "invite" -> {
                    UUID sender = UUID.fromString(data.get("from").getAsString());
                    Player partner = findPlayer(client, sender);
                    if (partner == null || client.player.distanceToSqr(partner) > MAX_INVITE_DISTANCE_SQUARED) {
                        JsonObject reject = new JsonObject();
                        reject.addProperty("type", "decline");
                        reject.addProperty("inviteId", data.get("inviteId").getAsString());
                        send(reject);
                        return;
                    }
                    incoming = new Invitation(data.get("inviteId").getAsString(), sender, System.currentTimeMillis() + 20_000);
                    message(client, partner.getName().getString() + " приглашает на танго. Нажми J (принять) или N (отказаться).");
                }
                case "invite_sent" -> message(client, "Приглашение отправлено, ожидаем ответ.");
                case "declined" -> {
                    targetPlayer = null;
                    message(client, "Приглашение отклонено или истекло.");
                }
                case "expired" -> {
                    incoming = null;
                    message(client, "Время приглашения истекло.");
                }
                case "start" -> {
                    UUID leader = UUID.fromString(data.get("leader").getAsString());
                    UUID follower = UUID.fromString(data.get("follower").getAsString());
                    UUID me = client.player.getUUID();
                    UUID partnerId = me.equals(leader) ? follower : me.equals(follower) ? leader : null;
                    if (partnerId == null) return;
                    Player partner = findPlayer(client, partnerId);
                    if (partner == null || client.player.distanceToSqr(partner) > MAX_INVITE_DISTANCE_SQUARED) {
                        cancel();
                        message(client, "Танец отменён: партнёр слишком далеко.");
                        return;
                    }
                    long startAtServer = data.get("startAt").getAsLong();
                    long localStartAt = startAtServer - serverClockOffsetMillis;
                    long delayMillis = Math.max(0, localStartAt - System.currentTimeMillis());
                    PairedAnimationsClient.CINEMATIC.schedule(client, partner, me.equals(leader), delayMillis);
                    incoming = null;
                    targetPlayer = partnerId;
                    message(client, "Танго начнётся через несколько секунд. K — остановить.");
                }
                case "cancelled" -> {
                    PairedAnimationsClient.CINEMATIC.stop(client);
                    targetPlayer = null;
                    message(client, "Совместный танец завершён.");
                }
                case "error" -> message(client, "Ошибка relay: " + data.get("reason").getAsString());
                default -> { }
            }
        } catch (RuntimeException invalidPacket) {
            message(client, "Получен некорректный пакет relay.");
        }
    }

    private final class Receiver implements WebSocket.Listener {
        private final Minecraft client;
        private final long attempt;
        private final StringBuilder messageBuffer = new StringBuilder();

        private Receiver(Minecraft client, long attempt) {
            this.client = client;
            this.attempt = attempt;
        }

        @Override public void onOpen(WebSocket webSocket) {
            helloSentMillis = System.currentTimeMillis();
            JsonObject hello = new JsonObject();
            hello.addProperty("type", "hello");
            hello.addProperty("uuid", localId.toString());
            hello.addProperty("scope", serverScope);
            webSocket.sendText(hello.toString(), true);
            webSocket.request(1);
        }

        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (messageBuffer.length() > 2048) {
                messageBuffer.setLength(0);
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "too long");
                return CompletableFuture.completedFuture(null);
            }
            if (last) {
                String complete = messageBuffer.toString();
                messageBuffer.setLength(0);
                client.execute(() -> {
                    if (generation == attempt) handle(client, complete);
                });
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            client.execute(() -> {
                if (generation == attempt) disconnect();
            });
            return CompletableFuture.completedFuture(null);
        }

        @Override public void onError(WebSocket webSocket, Throwable error) {
            client.execute(() -> {
                if (generation == attempt) disconnect();
            });
        }
    }

    private record Invitation(String id, UUID from, long expiresAtMillis) {}
}
