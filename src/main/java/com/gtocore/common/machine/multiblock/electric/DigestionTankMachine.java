package com.gtocore.common.machine.multiblock.electric;

import com.gtolib.api.machine.feature.multiblock.IFluidRendererMachine;
import com.gtolib.api.machine.multiblock.CoilMultiblockMachine;
import com.gtolib.utils.MachineUtils;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

import com.gto.datasynclib.annotations.SyncToClient;
import com.gto.fastcollection.OpenCacheHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class DigestionTankMachine extends CoilMultiblockMachine implements IFluidRendererMachine {

    @SyncToClient(notifyUpdate = true)
    private final Set<BlockPos> fluidBlockOffsets = new OpenCacheHashSet<>();
    @SyncToClient
    private Fluid cachedFluid;

    public DigestionTankMachine(MetaMachineBlockEntity holder) {
        super(holder, false, true);
    }

    @Override
    public void beforeWorking(@NotNull RecipeHandlerUnit unit, @NotNull GTRecipe recipe) {
        cachedFluid = IFluidRendererMachine.getFluid(recipe);
        super.beforeWorking(unit, recipe);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (!fluidBlockOffsets.isEmpty()) return;
        saveOffsets();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        fluidBlockOffsets.clear();
    }

    private void saveOffsets() {
        BlockPos pos = getPos();
        Direction facing = getFrontFacing();
        for (int y = 1; y < 3; y++) {
            for (int depth = 1; depth < 6; depth++) {
                int radius = (depth == 1 || depth == 5) ? 1 : 2;
                addLayerOffsets(pos, facing, depth, y, radius);
            }
        }
        for (int depth = 2; depth < 5; depth++) {
            addLayerOffsets(pos, facing, depth, 3, 1);
        }
    }

    private void addLayerOffsets(BlockPos pos, Direction facing, int depth, int y, int radius) {
        for (int lateral = -radius; lateral <= radius; lateral++) {
            fluidBlockOffsets.add(MachineUtils.getOffsetPos(depth, y, lateral, facing, pos).subtract(pos));
        }
    }

    @Override
    public Set<BlockPos> getFluidBlockOffsets() {
        return this.fluidBlockOffsets;
    }

    @Override
    public Fluid getCachedFluid() {
        return this.cachedFluid;
    }
}
