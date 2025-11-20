package org.ricey_yam.lynxmind.client.utils.game_ext.block;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;

import java.util.List;

@Getter
@Setter
public class BlockLite {
    private String id;
    private int x;
    private int y;
    private int z;

    public BlockLite(BlockPos blockPos) {
        this.id = BlockUtils.getBlockID(blockPos);
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
    }
    public BlockLite(String id, int x, int y, int z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
