package com.gtocore.common.pipe.mana;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public final class ManaPipeNet extends PipeNet<ManaPipeProperties> {

    private final Long2ObjectOpenHashMap<ManaRoutePath> netData = new Long2ObjectOpenHashMap<>();

    public ManaPipeNet(LevelPipeNet<ManaPipeProperties, ? extends PipeNet<ManaPipeProperties>> world) {
        super(world);
    }

    @Nullable
    public ManaRoutePath getNetData(long pipePos, BlockPos pos) {
        var path = netData.get(pipePos);
        if (path != null) return path;
        path = ManaNetWalker.createNetData(this, pos);
        if (path == null) return null;
        netData.put(pipePos, path);
        return path;
    }

    @Override
    public void onNeighbourUpdate(BlockPos fromPos) {
        netData.clear();
    }

    @Override
    public void onPipeConnectionsUpdate() {
        netData.clear();
    }

    @Override
    protected void transferNodeData(Long2ObjectOpenHashMap<Node<ManaPipeProperties>> transferredNodes,
                                    PipeNet<ManaPipeProperties> parentNet) {
        super.transferNodeData(transferredNodes, parentNet);
        netData.clear();
        ((ManaPipeNet) parentNet).netData.clear();
    }

    @Override
    protected void writeNodeData(ManaPipeProperties nodeData, CompoundTag tagCompound) {}

    @Override
    protected ManaPipeProperties readNodeData(CompoundTag tagCompound) {
        return ManaPipeProperties.INSTANCE;
    }
}
