package com.gtocore.common.pipe.mana;

import com.gtocore.common.blockentity.ManaPipeBlockEntity;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.pipenet.PipeNetWalker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;

public final class ManaNetWalker extends PipeNetWalker<ManaPipeBlockEntity, ManaPipeProperties, ManaPipeNet> {

    @Nullable
    public static ManaRoutePath createNetData(ManaPipeNet world, BlockPos sourcePipe) {
        ManaNetWalker walker = new ManaNetWalker(world, sourcePipe, 1);
        walker.sourcePipe = sourcePipe;
        walker.traversePipeNet();
        return walker.isFailed() ? null : walker.routePath;
    }

    private ManaRoutePath routePath;
    private BlockPos sourcePipe;

    private ManaNetWalker(ManaPipeNet world, BlockPos sourcePipe, int distance) {
        super(world, sourcePipe, distance);
    }

    @Override
    protected @NotNull PipeNetWalker<ManaPipeBlockEntity, ManaPipeProperties, ManaPipeNet> createSubWalker(ManaPipeNet world,
                                                                                                           Direction facingToNextPos,
                                                                                                           BlockPos nextPos,
                                                                                                           int walkedBlocks) {
        ManaNetWalker walker = new ManaNetWalker(world, nextPos, walkedBlocks);
        walker.sourcePipe = sourcePipe;
        return walker;
    }

    @Override
    protected void checkPipe(ManaPipeBlockEntity pipeTile, BlockPos pos) {}

    @Override
    protected void checkNeighbour(ManaPipeBlockEntity pipeTile, BlockPos pipePos, Direction faceToNeighbour,
                                  @Nullable BlockEntity neighbourTile) {
        if (neighbourTile == null || (pipePos.equals(sourcePipe))) return;
        if (((ManaNetWalker) root).routePath == null) {
            var opposite = faceToNeighbour.getOpposite();
            if (GTCapabilityHelper.getBlockEntityCapability(BotaniaForgeCapabilities.MANA_RECEIVER, neighbourTile, opposite) != null) {
                ((ManaNetWalker) root).routePath = new ManaRoutePath(pipeTile, faceToNeighbour, getWalkedBlocks());
                stop();
            }
        }
    }

    @Override
    protected Class<ManaPipeBlockEntity> getBasePipeClass() {
        return ManaPipeBlockEntity.class;
    }
}
