package com.gtocore.common.machine.multiblock.part;

import com.gtocore.api.machine.IHeatContainerPart;
import com.gtocore.api.machine.IVacuumPartMachine;

import com.gtolib.api.machine.heat.trait.NotifiableHeatContainer;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.handler.IO;

import com.gto.datasynclib.annotations.SaveToDisk;
import lombok.Getter;

public class TempVacuumInterfacePartMachine extends MultiblockPartMachine implements IHeatContainerPart, IVacuumPartMachine {

    @Getter
    @SaveToDisk
    private final NotifiableHeatContainer heatContainer;

    public TempVacuumInterfacePartMachine(MetaMachineBlockEntity holder) {
        super(holder);
        heatContainer = new NotifiableHeatContainer(this, IO.IN, 3600, 1, 24, 0.1);
        heatContainer.handler.setSideIOCondition(s -> s == getFrontFacing());
    }
}
