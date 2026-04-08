package com.lx862.takeaseat;

import com.lx862.takeaseat.config.Config;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SittingManager {
    private static final Map<BlockPos, UUID> playersSitting = new HashMap<>();
    public static boolean sitPendingHack = false;

    public static InteractionResult onBlockUse(Player player, Level world, InteractionHand hand, BlockHitResult blockHitResult) {
        if(world.isClientSide() || player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos hittedBlockPos = blockHitResult.getBlockPos();
        BlockState blockState = world.getBlockState(hittedBlockPos);
        boolean canSit = playerCanSit(player, world, hittedBlockPos, blockState);

        if(!canSit) {
            return InteractionResult.PASS;
        } else {
            sitPendingHack = true;
            addPlayerToSeat(world, blockState, hittedBlockPos, player);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    public static void addPlayerToSeat(Level world, BlockState seatBlockState, BlockPos seatPos, Player player) {
        if(player.isPassenger()) {
            player.removeVehicle();
        }

        Vec3 seatEntityPos = getSeatPosition(world, seatBlockState, seatPos);
        Entity sitEntity = spawnSeatEntity(world, seatEntityPos, seatPos);
        player.startRiding(sitEntity);

        playersSitting.put(seatPos, player.getUUID());
    }

    private static boolean playerCanSit(Player player, Level world, BlockPos hittedBlockPos, BlockState blockState) {
        Block block = blockState.getBlock();
        Identifier blockId = Util.getBlockId(block);
        Config config = TakeASeat.getConfig();
        if(player.isSpectator()) return false;

        if(!Permissions.check(player, "takeaseat.sit", config.requiredOpLevel())) {
            TakeASeat.LOGGER.debug("[TakeASeat] Player don't have permission to sit.");
            return false;
        }

        if(playersSitting.containsKey(hittedBlockPos)) {
            TakeASeat.LOGGER.debug("[TakeASeat] The seat has already been occupied.");
            return false;
        }

        if(config.mustBeEmptyHandToSit() && !Util.playerHandIsEmpty(player)) {
            TakeASeat.LOGGER.debug("[TakeASeat] Player is holding something.");
            return false;
        }

        if(!config.blockIdIsAllowed(blockId) && !blockInTag(blockState)) {
            TakeASeat.LOGGER.debug("[TakeASeat] Block is not allowed to sit.");
            return false;
        }

        if(config.blockMustBeLowerThanPlayer() && hittedBlockPos.getY() - 0.5 > player.getY()) {
            TakeASeat.LOGGER.debug("[TakeASeat] Seat Block is higher than the player.");
            return false;
        }

        if(config.maxDistance() > 0 && Util.euclideanDistance(hittedBlockPos, player.blockPosition()) > config.maxDistance()) {
            TakeASeat.LOGGER.debug("[TakeASeat] Player is too far from seat.");
            return false;
        }

        if(config.ensurePlayerWontSuffocate() && blockAboveCanSuffocate(world, hittedBlockPos)) {
            TakeASeat.LOGGER.debug("[TakeASeat] Player would suffocate if they tried to sit.");
            return false;
        }

        if(config.mustNotBeObstructed() && hasObstruction(world, hittedBlockPos, player.getEyePosition())) {
            TakeASeat.LOGGER.debug("[TakeASeat] There's a block between the player and the seat.");
            return false;
        }

        return true;
    }

    private static boolean blockAboveCanSuffocate(Level world, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState blockState = world.getBlockState(abovePos);
        return blockState.isSuffocating(world, abovePos);
    }

    private static boolean blockInTag(BlockState blockState) {
        for(TagKey<Block> tag : TakeASeat.getConfig().getAllowedBlockTag()) {
            if(blockState.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasObstruction(Level world, BlockPos targetBlockPos, Vec3 playerPos) {
        /* Jank code begins! */
        Vec3 targetPos = Util.toVec3d(targetBlockPos);
        BlockPos playerBlockPos = Util.toBlockPos(playerPos.x, playerPos.y, playerPos.z);
        double distance = Util.euclideanDistance(targetPos, playerPos, false);
        double increment = 1 / distance;
        double lerpProgress = 0;
        double lowestY = Math.min(targetPos.y, playerPos.y);
        double highestY = Math.max(targetPos.y, playerPos.y);
        double yDifference = highestY - lowestY;

        while(lerpProgress < 1) {
            lerpProgress += increment;

            for(int i = 0; i < yDifference; i++) {
                Vec3 lerped = targetPos.lerp(playerPos, lerpProgress);
                BlockPos finalPos = Util.toBlockPos(lerped.x, lowestY + i, lerped.z);
                if(Util.equalXZBlockPos(playerBlockPos, finalPos) || finalPos.equals(targetBlockPos)) continue;

                BlockState blockState = world.getBlockState(finalPos);
                if(blockState.getCollisionShape(world, finalPos) != Shapes.empty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Vec3 getSeatPosition(Level world, BlockState blockState, BlockPos pos) {
        Vec3 centeredBlockPos = Vec3.atBottomCenterOf(pos);
        if(blockState.getBlock() instanceof StairBlock) {
            Direction dir = blockState.getValue(StairBlock.FACING);
            Half half = blockState.getValue(StairBlock.HALF);
            double offsetX = dir.getStepX() * 0.25;
            double offsetY = half == Half.TOP ? 0.5 : 0;
            double offsetZ = dir.getStepZ() * 0.25;

            if(!TakeASeat.getConfig().stairs025Offset()) {
                offsetX = 0;
                offsetZ = 0;
            }

            centeredBlockPos = new Vec3(centeredBlockPos.x() - offsetX, centeredBlockPos.y() + offsetY, centeredBlockPos.z() - offsetZ);
        }

        if(blockState.getBlock() instanceof SlabBlock) {
            SlabType slabType = blockState.getValue(SlabBlock.TYPE);
            if(slabType == SlabType.TOP || slabType == SlabType.DOUBLE) {
                centeredBlockPos = new Vec3(centeredBlockPos.x(), pos.getY() + 0.5, centeredBlockPos.z());
            }
        }

        if(blockState.isCollisionShapeFullBlock(world, pos)) {
            centeredBlockPos = new Vec3(centeredBlockPos.x(), pos.getY() + 0.5, centeredBlockPos.z());
        }

        return centeredBlockPos;
    }

    /**
     * Spawn a seat entity (An invisible, invulnerable, area effect cloud)
     * @param world The world
     * @param pos A Vec3 position that the entity should spawn
     * @return The entity for the player to be ridden.
     */
    public static Entity spawnSeatEntity(Level world, Vec3 pos, BlockPos seatPos) {
        AreaEffectCloud sitEntity = new AreaEffectCloud(world, pos.x(), pos.y(), pos.z()) {
            @Override
            public void tick() {
                Entity firstPassenger = getFirstPassenger();
                if(firstPassenger == null || world.getBlockState(seatPos).isAir()) {
                    playersSitting.remove(seatPos);
                    this.kill((ServerLevel)world);
                }

                super.tick();
            }
        };
        sitEntity.setNoGravity(true);
        sitEntity.setInvulnerable(true);
        sitEntity.setInvisible(true);
        sitEntity.setWaitTime(0);
        sitEntity.setRadius(0);
        sitEntity.setDuration(Integer.MAX_VALUE);
        world.addFreshEntity(sitEntity);
        return sitEntity;
    }
}