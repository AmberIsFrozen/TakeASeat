package com.lx862.takeaseat.mixin;

import com.lx862.takeaseat.SittingManager;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** not very good hack to cancel out the building limit message when a player is teleporting. you have every right to blame me if your mod or some vanilla function broke. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendBuildLimitMessage(ZI)V", ordinal = 5), cancellable = true)
    private void takeaseat$cancelFalseBuildLimitMessage(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if(SittingManager.sitPendingHack) {
            ci.cancel();
            SittingManager.sitPendingHack = false;
        }
    }
}
