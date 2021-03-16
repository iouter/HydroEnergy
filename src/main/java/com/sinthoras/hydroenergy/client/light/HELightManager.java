package com.sinthoras.hydroenergy.client.light;

import com.sinthoras.hydroenergy.HE;
import com.sinthoras.hydroenergy.HEUtil;
import com.sinthoras.hydroenergy.client.HEClient;
import com.sinthoras.hydroenergy.blocks.HEWater;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Stack;

@SideOnly(Side.CLIENT)
public class HELightManager {

    private static float[] renderedWaterLevel = new float[HE.maxControllers];

    private static final HashMap<Long, HELightChunk> chunks = new HashMap<Long, HELightChunk>();
    private static final Stack<HELightChunk> availableBuffers = new Stack<HELightChunk>();

    public static void onChunkUnload(int chunkX, int chunkZ) {
        long key = HEUtil.chunkCoordsToKey(chunkX, chunkZ);
        if(chunks.containsKey(key)) {
            HELightChunk lightChunk = chunks.get(key);
            lightChunk.reset();
            availableBuffers.push(lightChunk);
            chunks.remove(key);
        }
    }

    public static void onChunkDataLoad(Chunk chunk, int subChunkHasDataFlags) {
        HELightChunk lightChunk = null;
        if(availableBuffers.empty()) {
            lightChunk = new HELightChunk();
        }
        else {
            lightChunk = availableBuffers.pop();
        }

        lightChunk.parseChunk(chunk, subChunkHasDataFlags);

        int chunkX = chunk.xPosition;
        int chunkZ = chunk.zPosition;
        long key = HEUtil.chunkCoordsToKey(chunkX, chunkZ);
        chunks.put(key, lightChunk);

        for(int chunkY=0;chunkY<16;chunkY++) {
            lightChunk.patch(chunk, chunkY);
        }
    }

    public static void onSetBlock(int blockX, int blockY, int blockZ, Block block, Block oldBlock) {
        if(block instanceof  HEWater) {
            int waterId = ((HEWater)block).getWaterId();
            int chunkX = HEUtil.coordBlockToChunk(blockX);
            int chunkZ = HEUtil.coordBlockToChunk(blockZ);
            long key = HEUtil.chunkCoordsToKey(chunkX, chunkZ);
            chunks.get(key).addWaterBlock(blockX, blockY, blockZ, waterId);
        }
        else if(oldBlock instanceof HEWater) {
            int chunkX = HEUtil.coordBlockToChunk(blockX);
            int chunkZ = HEUtil.coordBlockToChunk(blockZ);
            long key = HEUtil.chunkCoordsToKey(chunkX, chunkZ);
            chunks.get(key).removeWaterBlock(blockX, blockY, blockZ);
        }
    }

    public static void onPreRender(World world, int blockX, int blockY, int blockZ) {
        int chunkX = HEUtil.coordBlockToChunk(blockX);
        int chunkY = HEUtil.coordBlockToChunk(blockY);
        int chunkZ = HEUtil.coordBlockToChunk(blockZ);
        long key = HEUtil.chunkCoordsToKey(chunkX, chunkZ);
        HELightChunk lightChunk = chunks.get(key);
        lightChunk.patch(world.getChunkFromChunkCoords(chunkX, chunkZ), chunkY);
    }

    public static void onUpdateWaterLevels() {
        RenderGlobal renderGlobal = Minecraft.getMinecraft().renderGlobal;
        float[] newWaterLevels = HEClient.getAllWaterLevelsForRendering();
        for(int id=0;id<renderedWaterLevel.length;id++) {
            if(Math.abs(renderedWaterLevel[id] - newWaterLevels[id]) > (0.5f / HE.waterBlocks[0].getLightOpacity())) {
                renderedWaterLevel = newWaterLevels;
                for(long key : chunks.keySet()) {
                    HELightChunk chunk = chunks.get(key);
                    int chunkX = (int)(key >> 32);
                    int chunkZ = (int)key;

                    long keyWest = HEUtil.chunkCoordsToKey(chunkX - 1, chunkZ);
                    long keyNorth = HEUtil.chunkCoordsToKey(chunkX, chunkZ - 1);
                    long keyEast = HEUtil.chunkCoordsToKey(chunkX + 1, chunkZ);
                    long keySouth = HEUtil.chunkCoordsToKey(chunkX, chunkZ + 1);
                    HELightChunk neighborChunkWest = chunks.get(keyWest);
                    HELightChunk neighborChunkNorth = chunks.get(keyNorth);
                    HELightChunk neighborChunkEast = chunks.get(keyEast);
                    HELightChunk neighborChunkSouth = chunks.get(keySouth);

                    for(int chunkY=0;chunkY<chunk.requiresPatching.length;chunkY++) {
                        if(chunk.subChunkHasWaterFlags[chunkY]) {
                            chunk.requiresPatching[chunkY] = true;
                            int blockX = HEUtil.coordChunkToBlock(chunkX);
                            int blockY = HEUtil.coordChunkToBlock(chunkY);
                            int blockZ = HEUtil.coordChunkToBlock(chunkZ);
                            renderGlobal.markBlocksForUpdate(blockX, blockY, blockZ, blockX + 15, blockY + 15, blockZ + 15);

                            // Handle neighbors that don't have water, but touch it
                            // Technically, a chunk like this could be surrounded by chunks with water and receive multiple
                            // updates, but this scenario is rather unlikely and therefore, not worth checking for.
                            if(neighborChunkWest == null && chunk.neighborRequiresPatchingWest[chunkY]) {
                                renderGlobal.markBlocksForUpdate(blockX - 16, blockY, blockZ, blockX - 1, blockY + 15, blockZ + 15);
                            }
                            if(neighborChunkNorth == null && chunk.neighborRequiresPatchingNorth[chunkY]) {
                                renderGlobal.markBlocksForUpdate(blockX, blockY, blockZ - 16, blockX + 15, blockY + 15, blockZ - 1);
                            }
                            if(neighborChunkEast == null && chunk.neighborRequiresPatchingEast[chunkY]) {
                                renderGlobal.markBlocksForUpdate(blockX + 16, blockY, blockZ, blockX + 31, blockY + 15, blockZ + 15);
                            }
                            if(neighborChunkSouth == null && chunk.neighborRequiresPatchingSouth[chunkY]) {
                                renderGlobal.markBlocksForUpdate(blockX, blockY, blockZ + 16, blockX + 15, blockY + 15, blockZ + 31);
                            }
                        }
                    }

                }
                return;
            }
        }
    }
}


@SideOnly(Side.CLIENT)
class HELightChunk {
    public BitSet[] lightFlags;
    public boolean[] subChunkHasWaterFlags;
    public boolean[] requiresPatching;
    public boolean[] neighborRequiresPatchingWest;
    public boolean[] neighborRequiresPatchingNorth;
    public boolean[] neighborRequiresPatchingEast;
    public boolean[] neighborRequiresPatchingSouth;
    // Holds corresponding waterId for X/Z combination. I don't expect people to stack
    // multiple on top of each other. If they do the light calculation will be incorrect.
    // Acceptable to save quite some RAM.
    public int[][] waterIds;


    public HELightChunk() {
        lightFlags = new BitSet[16];
        for(int chunkY=0;chunkY<lightFlags.length;chunkY++) {
            lightFlags[chunkY] = new BitSet(16 * 16 * 16);
        }

        waterIds = new int[16][16];
        subChunkHasWaterFlags = new boolean[16];
        requiresPatching = new boolean[16];

        // If a block at the chunk border is from water it means that the neighbors need to be handled as well
        neighborRequiresPatchingWest = new boolean[16];
        neighborRequiresPatchingNorth = new boolean[16];
        neighborRequiresPatchingEast = new boolean[16];
        neighborRequiresPatchingSouth = new boolean[16];
    }

    public void reset() {
        for(int chunkY=0;chunkY<16;chunkY++) {
            lightFlags[chunkY].clear();
            subChunkHasWaterFlags[chunkY] = false;
            neighborRequiresPatchingWest[chunkY] = false;
            neighborRequiresPatchingNorth[chunkY] = false;
            neighborRequiresPatchingEast[chunkY] = false;
            neighborRequiresPatchingSouth[chunkY] = false;
            // waterIds does not need to be reset since it is only accessed
            // whenever data is found and for that to happen there must be a
            // valid value in it again
        }
    }

    // This method checks for each block in the chunk what block it is
    // with the logic from ExtendedBlockStorage.getBlockByExtId(blockX, blockY, blockZ)
    // and a waterId LUT (getWaterIdFromBlockId)
    public void parseChunk(Chunk chunk, int subChunkHasDataFlags) {
        ExtendedBlockStorage[] chunkStorage = chunk.getBlockStorageArray();
        for(int chunkY=0;chunkY<16;chunkY++) {
            if((subChunkHasDataFlags & (1 << chunkY)) != 0) {
                ExtendedBlockStorage subChunkStorage = chunkStorage[chunkY];
                BitSet flags = lightFlags[chunkY];
                byte[] LSB = subChunkStorage.getBlockLSBArray();
                NibbleArray MSB = subChunkStorage.getBlockMSBArray();

                int[] bucketsBlockX = new int[16];
                int[] bucketsBlockZ = new int[16];

                for (int blockX = 0; blockX < 16; blockX++) {
                    for (int blockY = 0; blockY < 16; blockY++) {
                        for (int blockZ = 0; blockZ < 16; blockZ++) {
                            int blockId = LSB[blockY << 8 | blockZ << 4 | blockX] & 255;
                            if (MSB != null) {
                                blockId |= MSB.get(blockX, blockY, blockZ) << 8;
                            }
                            int waterId = getWaterIdFromBlockId(blockId);
                            if (waterId >= 0) {
                                bucketsBlockX[blockX]++;
                                bucketsBlockZ[blockZ]++;
                                flags.set((blockX << 8) | (blockY << 4) | blockZ);
                                waterIds[blockX][blockZ] = waterId;
                                this.subChunkHasWaterFlags[chunkY] = true;
                            }
                        }
                    }
                }

                neighborRequiresPatchingWest[chunkY] = bucketsBlockX[0] > 0;
                neighborRequiresPatchingNorth[chunkY] = bucketsBlockZ[0] > 0;
                neighborRequiresPatchingEast[chunkY] = bucketsBlockX[15] > 0;
                neighborRequiresPatchingSouth[chunkY] = bucketsBlockZ[15] > 0;
            }
        }
        requiresPatching = subChunkHasWaterFlags.clone();
    }

    public void removeWaterBlock(int blockX, int blockY, int blockZ) {
        BitSet flags = lightFlags[blockY >> 4];
        blockX = blockX & 15;
        blockY = blockY & 15;
        blockZ = blockZ & 15;
        flags.clear((blockX << 8) | (blockY << 4) | blockZ);
    }

    public void addWaterBlock(int blockX, int blockY, int blockZ, int waterId) {
        int chunkY = HEUtil.coordBlockToChunk(blockY);
        BitSet flags = lightFlags[chunkY];
        this.subChunkHasWaterFlags[chunkY] = true;
        blockX = blockX & 15;
        blockY = blockY & 15;
        blockZ = blockZ & 15;
        flags.set((blockX << 8) | (blockY << 4) | blockZ);
        waterIds[blockX][blockZ] = waterId;
    }

    public void patch(Chunk chunk, int chunkY) {
        if(subChunkHasWaterFlags[chunkY] && requiresPatching[chunkY])  {
            float[] waterLevels = HEClient.getAllWaterLevelsForRendering();
            BitSet flags = lightFlags[chunkY];
            NibbleArray skyLightArray = chunk.getBlockStorageArray()[chunkY].getSkylightArray();
            for (int linearCoord = flags.nextSetBit(0); linearCoord != -1; linearCoord = flags.nextSetBit(linearCoord + 1)) {
                int blockX = linearCoord >> 8;
                int blockY = (linearCoord >> 4) & 15;
                int blockZ = linearCoord & 15;
                int waterId = waterIds[blockX][blockZ];
                float diff = Math.min((chunkY << 4) - waterLevels[waterId] + blockY, 0);
                int lightVal = (int)(15 + diff * HE.waterBlocks[0].getLightOpacity());
                lightVal = Math.max(lightVal, 0);
                skyLightArray.set(blockX, blockY, blockZ, lightVal);
            }
            requiresPatching[chunkY] = false;
        }
    }

    private static int getWaterIdFromBlockId(int blockId) {
        for(int i=0;i<HE.waterBlockIds.length;i++) {
            if(HE.waterBlockIds[i] == blockId) {
                return i;
            }
        }
        return -1;
    }
}