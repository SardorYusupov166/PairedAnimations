package dev.sardoryusupov.pairedanimations.mixin;

import dev.sardoryusupov.pairedanimations.CinematicSession;
import dev.sardoryusupov.pairedanimations.PairedAnimationsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.2 no longer has Camera.setup(Level, Entity, ...).
 * Camera.update(DeltaTracker) recalculates the vanilla camera every frame.
 * Override only the local rendering camera after that vanilla update.
 * Vanilla 26.2 has setRotation(yaw, pitch); the 3-float overload is a NeoForge patch.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("TAIL"))
    private void pairedanimations$applyCutscene(DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!PairedAnimationsClient.CINEMATIC.isActive() || client.player == null) return;

        // Do not modify other camera entities or the real player's movement.
        Camera camera = (Camera) (Object) this;
        if (camera.entity() != client.player) return;

        CinematicSession.CameraShot shot = PairedAnimationsClient.CINEMATIC.cameraShot(
            client, camera.getCameraEntityPartialTicks(deltaTracker)
        );
        if (shot == null) return;

        setPosition(shot.position().x, shot.position().y, shot.position().z);
        setRotation(shot.yaw(), shot.pitch());
    }
}
