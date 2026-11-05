package dev.sardoryusupov.pairedanimations.mixin;

import dev.sardoryusupov.pairedanimations.PairedAnimationsClient;
import dev.sardoryusupov.pairedanimations.TangoTimeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
    @Shadow @Final public ModelPart leftSleeve;
    @Shadow @Final public ModelPart rightSleeve;
    @Shadow @Final public ModelPart leftPants;
    @Shadow @Final public ModelPart rightPants;
    @Shadow @Final public ModelPart jacket;

    protected PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void pairedanimations$applyPose(AvatarRenderState renderState, CallbackInfo ci) {
        TangoTimeline.PoseFrame pose = PairedAnimationsClient.CINEMATIC.poseFor(
            renderState.id, Minecraft.getInstance()
        );
        if (pose == null) return;

        leftArm.xRot = pose.leftArmX();
        rightArm.xRot = pose.rightArmX();
        leftArm.yRot = pose.leftArmY();
        rightArm.yRot = pose.rightArmY();
        leftArm.zRot = pose.leftArmZ();
        rightArm.zRot = pose.rightArmZ();
        leftLeg.xRot = pose.leftLegX();
        rightLeg.xRot = pose.rightLegX();
        body.xRot = pose.bodyX();
        body.yRot = pose.bodyY();
        body.zRot = pose.bodyZ();
        head.xRot += pose.headX();

        // Vanilla skin overlay parts must mirror the bones after custom posing.
        copyRotation(leftArm, leftSleeve);
        copyRotation(rightArm, rightSleeve);
        copyRotation(leftLeg, leftPants);
        copyRotation(rightLeg, rightPants);
        copyRotation(body, jacket);
    }

    private static void copyRotation(ModelPart source, ModelPart target) {
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
    }
}
