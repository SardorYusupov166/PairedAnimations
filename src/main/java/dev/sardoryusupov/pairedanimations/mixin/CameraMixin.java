package dev.sardoryusupov.pairedanimations.mixin;

import dev.sardoryusupov.pairedanimations.CinematicSession;
import dev.sardoryusupov.pairedanimations.PairedAnimationsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void pairedanimations$applyCutscene(
        Level level, Entity entity, boolean detached, boolean thirdPersonReverse,
        float partialTick, CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (!PairedAnimationsClient.CINEMATIC.isActive() || client.player != entity) return;
        CinematicSession.CameraShot shot =
            PairedAnimationsClient.CINEMATIC.cameraShot(client, partialTick);
        if (shot == null) return;

        setPosition(shot.position().x, shot.position().y, shot.position().z);
        setRotation(shot.yaw(), shot.pitch());
    }
}
