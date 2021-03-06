package cn.nukkit.level.util;

import cn.nukkit.block.Block;
import cn.nukkit.level.GlobalBlockPalette;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.BinaryStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class PalettedBlockStorage {

    private static final int SIZE = 4096;

    private final IntList palette; // cache用fullId代替runtimeId
    private BitArray bitArray;

    public PalettedBlockStorage() {
        this(BitArrayVersion.V2);
    }

    public PalettedBlockStorage(BitArrayVersion version) {
        this(version, false);
    }

    public PalettedBlockStorage(boolean cache) {
        this(BitArrayVersion.V2, cache);
    }

    public PalettedBlockStorage(BitArrayVersion version, boolean cache) {
        this.bitArray = version.createPalette(SIZE);
        this.palette = new IntArrayList(16);
        if (!cache) {
            this.palette.add(GlobalBlockPalette.getOrCreateRuntimeId(Block.AIR, 0)); // Air is at the start of every palette.
        } else {
            this.palette.add(0);
        }
    }

    private PalettedBlockStorage(BitArray bitArray, IntList palette) {
        this.palette = palette;
        this.bitArray = bitArray;
    }

    private int getPaletteHeader(BitArrayVersion version, boolean runtime) {
        return (version.getId() << 1) | (runtime ? 1 : 0);
    }

    public void setBlock(int index, int runtimeId) {
        try {
            int id = this.idFor(runtimeId);
            this.bitArray.set(index, id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to set block runtime ID: " + runtimeId + ", palette: " + palette, e);
        }
    }

    public void writeTo(BinaryStream stream) {
        stream.putByte((byte) getPaletteHeader(bitArray.getVersion(), true));

        for (int word : bitArray.getWords()) {
            stream.putLInt(word);
        }

        stream.putVarInt(palette.size());
        palette.forEach((IntConsumer) stream::putVarInt);
    }

    public void writeToCache(BinaryStream stream) {
        stream.putByte((byte) getPaletteHeader(bitArray.getVersion(), false));

        for (int word : bitArray.getWords()) {
            stream.putLInt(word);
        }

        stream.putVarInt(palette.size());
        List<CompoundTag> tagList = new ArrayList<>();
        for (int legacyId : palette) {
            //tagList.add(GlobalBlockPalette.getState(runtimeId));
            tagList.add(new CompoundTag()
                    .putString("name", GlobalBlockPalette.getNameByBlockId(legacyId >> 4))
                    .putShort("val", legacyId & 0xf));
        }
        try {
            stream.put(NBTIO.write(tagList, ByteOrder.LITTLE_ENDIAN, true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onResize(BitArrayVersion version) {
        BitArray newBitArray = version.createPalette(SIZE);

        for (int i = 0; i < SIZE; i++) {
            newBitArray.set(i, this.bitArray.get(i));
        }
        this.bitArray = newBitArray;
    }

    private int idFor(int runtimeId) {
        int index = this.palette.indexOf(runtimeId);
        if (index != -1) {
            return index;
        }

        index = this.palette.size();
        BitArrayVersion version = this.bitArray.getVersion();
        if (index > version.getMaxEntryValue()) {
            BitArrayVersion next = version.next();
            if (next != null) {
                this.onResize(next);
            }
        }
        this.palette.add(runtimeId);
        return index;
    }

    public boolean isEmpty() {
        if (this.palette.size() == 1) {
            return true;
        }
        for (int word : this.bitArray.getWords()) {
            if (Integer.toUnsignedLong(word) != 0L) {
                return false;
            }
        }
        return true;
    }

    public PalettedBlockStorage copy() {
        return new PalettedBlockStorage(this.bitArray.copy(), new IntArrayList(this.palette));
    }
}
