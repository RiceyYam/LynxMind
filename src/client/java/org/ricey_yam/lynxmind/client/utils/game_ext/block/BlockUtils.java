package org.ricey_yam.lynxmind.client.utils.game_ext.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class BlockUtils {
    /// 搜索最近的方块的位置
    public static BlockPos findNearestBlock(LivingEntity livingEntity, List<String> targetBlockIDList, int radius, List<BlockPos> blackList) {
        if(livingEntity == null || targetBlockIDList == null || targetBlockIDList.isEmpty()) return null;
        var startPos = livingEntity.getBlockPos();
        var world = livingEntity.getEntityWorld();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int[][] directions = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            var blockName = getBlockID(currentPos);
            for (String targetName : targetBlockIDList) {
                if (targetName.equals(blockName) && !blackList.contains(currentPos)) {
                    return currentPos;
                }
            }
            if (currentPos.getManhattanDistance(startPos) > radius) {
                continue;
            }
            for (int[] dir : directions) {
                BlockPos neighborPos = currentPos.add(dir[0], dir[1], dir[2]);
                if (!visited.contains(neighborPos)) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                }
            }
        }

        return null;
    }
    public static BlockPos findNearestBlock(LivingEntity livingEntity, List<String> targetBlockIDList, int radius){
        return findNearestBlock(livingEntity, targetBlockIDList,radius,List.of());
    }

    /// 扫描附近的全部方块
    public static List<BlockLite> scanAllBlocks(LivingEntity livingEntity,List<String> targetBlockIDList, int radius){
        var entityPos = livingEntity.getBlockPos();
        var result = new ArrayList<BlockLite>();
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    var pos = entityPos.add(x, y, z);
                    var blockState = BlockUtils.getBlockState(pos);
                    if(targetBlockIDList.contains(BlockUtils.getBlockID(pos))){
                        result.add(new BlockLite(pos));
                    }
                }
            }
        }
        return result;
    }

    /// 寻找工作台的放置点
    public static BlockPos findCraftingTablePlacePoint(LivingEntity livingEntity, int range) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) return null;

        BlockPos playerPos = livingEntity.getBlockPos();
        BlockPos bestPos = null;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    World world = client.world;

                    if (!world.isInBuildLimit(pos)) {
                        continue;
                    }

                    if (!isInRange(playerPos, pos, range)) {
                        continue;
                    }

                    BlockState targetState = world.getBlockState(pos);
                    if(Blocks.CRAFTING_TABLE.getDefaultState().canPlaceAt(world, pos)) return pos;
                }
            }
        }
        return null;
    }

    /// 校验位置是否在 range 范围内（欧氏距离）
    private static boolean isInRange(BlockPos center, BlockPos target, int range) {
        return center.getSquaredDistance(target) <= range * range;
    }

    /// 获取方块状态
    public static BlockState getBlockState(BlockPos pos) {
        if (MinecraftClient.getInstance().world != null) {
            return MinecraftClient.getInstance().world.getBlockState(pos);
        }
        return null;
    }

    /// 获取方块
    public static Block getTargetBlock(BlockPos pos) {
        if (pos == null) return null;
        var state = BlockUtils.getBlockState(pos);
        return state != null ? state.getBlock() : null;
    }

    /// 获取方块ID
    public static String getBlockID(BlockPos pos) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || pos == null) {
            return null;
        }
        Block block = world.getBlockState(pos).getBlock();
        Identifier blockId = Registries.BLOCK.getId(block);
        return blockId.toString();
    }
    public static String getBlockID(Block block) {
        if(block == null) return null;
        Identifier blockId = Registries.BLOCK.getId(block);
        return blockId.toString();
    }

}
