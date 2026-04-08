package com.lx862.takeaseat;

import com.google.gson.JsonArray;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class Util {
    public static Identifier getBlockId(Block block) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
    }

    public static double euclideanDistance(BlockPos pos1, BlockPos pos2) {
        int x1 = pos1.getX();
        int x2 = pos2.getX();
        int z1 = pos1.getZ();
        int z2 = pos2.getZ();
        int yDifference = Math.abs(pos2.getY() - pos1.getY());
        return yDifference + Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2));
    }

    public static double euclideanDistance(Vec3 pos1, Vec3 pos2, boolean accountForY) {
        double x1 = pos1.x;
        double x2 = pos2.x;
        double z1 = pos1.z;
        double z2 = pos2.z;
        double yDifference = accountForY ? Math.abs(pos2.y() - pos1.y()) : 0;
        return yDifference + (Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(z2 - z1, 2)));
    }

    public static Vec3 toVec3d(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos toBlockPos(double x, double y, double z) {
        return new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    public static <T> JsonArray toJsonArray(Collection<T> iterable, Function<T, String> toString) {
        JsonArray array = new JsonArray();
        for(T object : iterable) {
            array.add(toString.apply(object));
        }
        return array;
    }

    public static boolean equalXZBlockPos(BlockPos pos1, BlockPos pos2) {
        return pos1.getX() == pos2.getX() && pos1.getZ() == pos2.getZ();
    }

    static boolean playerHandIsEmpty(Player player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }
}
