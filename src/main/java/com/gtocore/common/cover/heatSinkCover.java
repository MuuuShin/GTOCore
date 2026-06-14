package com.gtocore.common.cover;

import com.gtolib.api.capability.IHeatContainer;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.utils.TaskHandler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class heatSinkCover extends CoverBehavior {

    private TickableSubscription subscription;

    public heatSinkCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (coverHolder.getLevel() instanceof ServerLevel serverLevel) {
            TaskHandler.enqueueTask(serverLevel, () -> subscription = coverHolder.subscribeServerTick(subscription, this::update, 40));
        }
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private void update() {
        var container = GTCapabilityHelper.getBlockEntityGTCapability(IHeatContainer.class, coverHolder.holder(), null);
        if (container == null) return;
        var heat = container.getCurrentHeat();
        if (heat > 0 && coverHolder.holder().getNeighborBlockState(attachedSide).isAir()) {
            container.setCurrentHeat((long) Math.max(0, heat - (80 * container.getHeatCapacity())));
        }
    }
}
