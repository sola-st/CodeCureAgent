package cn.nukkit.blockentity;

import cn.nukkit.block.Block;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockEntityEnchantTable extends BlockEntitySpawnable implements BlockEntityNameable {

    private static final String CUSTOM_NAME = "CustomName";
    private static final String ENCHANTING_TABLE_NAME = "Enchanting Table";

    public BlockEntityEnchantTable(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public boolean isBlockEntityValid() {
        return getBlock().getId() == Block.ENCHANT_TABLE;
    }

    @Override
    public String getName() {
        return this.hasName() ? this.namedTag.getString(CUSTOM_NAME) : ENCHANTING_TABLE_NAME;
    }

    @Override
    public boolean hasName() {
        return this.namedTag.contains(CUSTOM_NAME);
    }

    @Override
    public void setName(String name) {
        if (name == null || name.equals("")) {
            this.namedTag.remove(CUSTOM_NAME);
            return;
        }

        this.namedTag.putString(CUSTOM_NAME, name);
    }

    @Override
    public CompoundTag getSpawnCompound() {
        CompoundTag c = new CompoundTag()
                .putString("id", BlockEntity.ENCHANT_TABLE)
                .putInt("x", (int) this.x)
                .putInt("y", (int) this.y)
                .putInt("z", (int) this.z);

        if (this.hasName()) {
            c.put(CUSTOM_NAME, this.namedTag.get(CUSTOM_NAME));
        }

        return c;
    }

}
