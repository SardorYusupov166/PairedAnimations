package dev.sardoryusupov.pairedanimations;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;

public final class PairedAnimationsClient implements ClientModInitializer {
    public static final CinematicSession CINEMATIC = new CinematicSession();
    private static final double MAX_PARTNER_DISTANCE = 6.0D;
    private KeyMapping previewKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("pairedanimations", "main")
        );
        previewKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.pairedanimations.preview",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CINEMATIC.tick(client);
            while (previewKey.consumeClick()) {
                togglePreview(client);
            }
        });
    }

    private static void togglePreview(Minecraft client) {
        if (CINEMATIC.isActive()) {
            CINEMATIC.stop(client);
            notify(client, "message.pairedanimations.stop");
            return;
        }

        if (client.player == null || client.level == null) return;

        Player partner = client.level.players().stream()
            .filter(player -> player != client.player)
            .filter(player -> player.isAlive() && !player.isSpectator())
            .filter(player -> player.distanceToSqr(client.player) <= MAX_PARTNER_DISTANCE * MAX_PARTNER_DISTANCE)
            .min(Comparator.comparingDouble(player -> player.distanceToSqr(client.player)))
            .orElse(null);

        if (partner == null) {
            notify(client, "message.pairedanimations.no_partner");
            return;
        }

        CINEMATIC.start(client, partner);
        notify(client, "message.pairedanimations.start");
        notify(client, "message.pairedanimations.preview_only");
    }

    private static void notify(Minecraft client, String key) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.translatable(key));
        }
    }
}
