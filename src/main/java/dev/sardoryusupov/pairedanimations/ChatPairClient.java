package dev.sardoryusupov.pairedanimations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Experimental MigosMC /msg synchronization. No external relay needed:
 * only normal, private command messages are sent through the game server.
 *
 * Every command is a single, small packet; choreography is computed locally.
 * The server may rate-limit, filter or reformat PMs. No packet bypass occurs.
 */
public final class ChatPairClient {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long INVITE_TTL_MS = 20_000;
    private static final long START_DELAY_MS = 6_000;
    private static final long COOLDOWN_MS = 3_000;
    private static final double MAX_DISTANCE_SQR = 36;
    private static final String PREFIX = "[PairedAnimations] ";

    private ClientLevel level;
    private Invitation incoming;
    private Pending outgoing;
    private Active active;
    private long nextInviteAt;

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.getCurrentServer() == null) {
            clear();
            return;
        }
        if (level != client.level) {
            clear();
            level = client.level;
        }
        long now = System.currentTimeMillis();
        if (incoming != null && now > incoming.expiresAt) incoming = null;
        if (outgoing != null && now > outgoing.expiresAt) {
            outgoing = null;
            status(client, "Ответ на приглашение не получен.");
        }
        if (active != null && !PairedAnimationsClient.CINEMATIC.isActive()) {
            // A cancelled/done cutscene is removed by cancel(), see client tick.
            active = null;
        }
    }

    public void invite(Minecraft client, Player partner) {
        if (!hasContext(client) || !validPartner(client, partner)) {
            status(client, "Партнёр должен быть в радиусе 6 блоков.");
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextInviteAt) {
            status(client, "Приглашения можно отправлять не чаще раза в 3 секунды.");
            return;
        }
        if (incoming != null || outgoing != null || active != null
            || PairedAnimationsClient.CINEMATIC.isActive()) {
            status(client, "Сначала заверши текущее приглашение или танец.");
            return;
        }
        nextInviteAt = now + COOLDOWN_MS;
        String nonce = String.format(Locale.ROOT, "%012x", RANDOM.nextLong() & 0xFFFFFFFFFFFFL);
        outgoing = new Pending(partner.getName().getString(), nonce, now + INVITE_TTL_MS);
        send(client, outgoing.partnerName, 'I', nonce, 0);
        status(client, "Приглашение на танго отправлено игроку " + outgoing.partnerName
            + " через /msg. Ожидаем ответ.");
    }

    /** K can cancel an outstanding chat invite before the dance begins. */
    public boolean cancelPending(Minecraft client) {
        if (outgoing != null) {
            if (hasContext(client)) send(client, outgoing.partnerName, 'C', outgoing.nonce, 0);
            outgoing = null;
            status(client, "Приглашение отменено.");
            return true;
        }
        if (incoming != null) {
            decline(client);
            return true;
        }
        return false;
    }

    public void accept(Minecraft client) {
        if (!hasContext(client) || incoming == null) {
            status(client, "Нет входящего приглашения через чат.");
            return;
        }
        if (incoming.accepted) {
            status(client, "Приглашение уже принято. Ожидаем старт.");
            return;
        }
        Player partner = findPlayer(client, incoming.partnerName);
        if (!validPartner(client, partner)) {
            decline(client);
            status(client, "Партнёр ушёл или находится слишком далеко.");
            return;
        }
        send(client, incoming.partnerName, 'A', incoming.nonce, 0);
        incoming.accepted = true;
        incoming.expiresAt = System.currentTimeMillis() + INVITE_TTL_MS;
        status(client, "Приглашение принято. Ожидаем команду начала танго.");
    }

    public void decline(Minecraft client) {
        if (incoming == null) {
            status(client, "Нет входящего приглашения через чат.");
            return;
        }
        if (hasContext(client)) {
            send(client, incoming.partnerName, incoming.accepted ? 'C' : 'D', incoming.nonce, 0);
        }
        incoming = null;
        status(client, "Приглашение отменено.");
    }

    /** Return true only for a correctly parsed incoming PA protocol PM. */
    public boolean onMessage(Minecraft client, String renderedText) {
        if (!hasContext(client)) return false;
        ChatPairProtocol.Parsed packet = ChatPairProtocol.parse(renderedText,
            client.player.getName().getString());
        if (packet == null) return false;
        Player sender = findPlayer(client, packet.senderName());
        if (sender == null) return false;
        long now = System.currentTimeMillis();
        String senderName = sender.getName().getString();

        switch (packet.operation()) {
            case 'I' -> {
                if (!validPartner(client, sender) || outgoing != null || active != null
                    || incoming != null || PairedAnimationsClient.CINEMATIC.isActive()) return true;
                incoming = new Invitation(senderName, packet.nonce(), now + INVITE_TTL_MS);
                status(client, senderName + " приглашает тебя на танго. J — принять, N — отказаться.");
            }
            case 'A' -> {
                if (outgoing == null || !outgoing.matches(senderName, packet.nonce())
                    || outgoing.expiresAt < now || !validPartner(client, sender)) return true;
                long startsAt = now + START_DELAY_MS;
                send(client, senderName, 'S', outgoing.nonce, startsAt);
                PairedAnimationsClient.CINEMATIC.schedule(client, sender, true, START_DELAY_MS);
                active = new Active(senderName, outgoing.nonce);
                outgoing = null;
                status(client, "Партнёр принял приглашение. Танец начнётся через 6 секунд.");
            }
            case 'S' -> {
                if (incoming == null || !incoming.accepted
                    || !incoming.matches(senderName, packet.nonce())
                    || !validPartner(client, sender)) return true;
                long delay = packet.startEpochMillis() - now;
                // Reject wildly desynchronized clocks, stale or spoofed packets.
                if (delay < 1000 || delay > 12000) {
                    status(client, "Время начала не совпадает. Проверь часы Windows обоих игроков.");
                    incoming = null;
                    send(client, senderName, 'C', packet.nonce(), 0);
                    return true;
                }
                PairedAnimationsClient.CINEMATIC.schedule(client, sender, false, delay);
                active = new Active(senderName, incoming.nonce);
                incoming = null;
                status(client, "Танго через " + Math.round(delay / 1000.0) + " сек. K — остановить.");
            }
            case 'D' -> {
                if (outgoing != null && outgoing.matches(senderName, packet.nonce())) {
                    outgoing = null;
                    status(client, "Партнёр отклонил приглашение.");
                } else if (active != null && active.matches(senderName, packet.nonce())) {
                    active = null;
                    PairedAnimationsClient.CINEMATIC.stop(client);
                    status(client, "Партнёр отменил танец.");
                }
            }
            case 'C' -> {
                if (active != null && active.matches(senderName, packet.nonce())) {
                    active = null;
                    PairedAnimationsClient.CINEMATIC.stop(client);
                    status(client, "Партнёр остановил танец.");
                } else if (outgoing != null && outgoing.matches(senderName, packet.nonce())) {
                    outgoing = null;
                } else if (incoming != null && incoming.matches(senderName, packet.nonce())) {
                    incoming = null;
                }
            }
            default -> { }
        }
        return true;
    }

    public void cancel(Minecraft client) {
        if (active != null && hasContext(client)) {
            send(client, active.partnerName, 'C', active.nonce, 0);
        }
        active = null;
    }

    public void clear() {
        level = null;
        active = null;
        incoming = null;
        outgoing = null;
    }

    private static Player findPlayer(Minecraft client, String name) {
        if (client.level == null || name == null) return null;
        for (Player player : client.level.players()) {
            if (player.getName().getString().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    private static boolean validPartner(Minecraft client, Player partner) {
        return client.player != null && partner != null && partner != client.player
            && partner.isAlive() && !partner.isSpectator()
            && client.player.distanceToSqr(partner) <= MAX_DISTANCE_SQR;
    }

    private static boolean hasContext(Minecraft client) {
        return client.player != null && client.level != null && client.getCurrentServer() != null;
    }

    private static void send(Minecraft client, String target, char op, String nonce, long startAt) {
        if (!hasContext(client) || !target.matches("[A-Za-z0-9_]{3,16}")) return;
        // All values are validated Minecraft player names / generated tokens.
        client.player.connection.sendCommand(
            "msg " + target + " " + ChatPairProtocol.encode(op, nonce, startAt)
        );
    }

    private static void status(Minecraft client, String message) {
        if (client.player != null)
            client.player.sendSystemMessage(Component.literal(PREFIX + message));
    }

    private static final class Invitation {
        final String partnerName;
        final String nonce;
        boolean accepted;
        long expiresAt;
        Invitation(String name, String nonce, long until) {
            this.partnerName = name;
            this.nonce = nonce;
            this.expiresAt = until;
        }
        boolean matches(String name, String token) {
            return partnerName.equalsIgnoreCase(name) && nonce.equals(token);
        }
    }
    private record Pending(String partnerName, String nonce, long expiresAt) {
        boolean matches(String name, String token) {
            return partnerName.equalsIgnoreCase(name) && nonce.equals(token);
        }
    }
    private record Active(String partnerName, String nonce) {
        boolean matches(String name, String token) {
            return partnerName.equalsIgnoreCase(name) && nonce.equals(token);
        }
    }
}
