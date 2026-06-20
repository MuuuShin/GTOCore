package com.gtocore.client.renderer.fx;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@OnlyIn(Dist.CLIENT)
public class FXManager {

    public static final List<AbstractFX> FX_LIST = new CopyOnWriteArrayList<>();

    public static void dispatchFXs(RenderLevelStageEvent event) {
        BlackHole.beginFrame();
        for (AbstractFX fx : FX_LIST) {
            fx.render(event.getStage(), event.getLevelRenderer(), event.getPoseStack(), event.getProjectionMatrix(), event.getPartialTick(), event.getCamera(), event.getFrustum());
        }
    }

    public static void tickFXs() {
        FX_LIST.removeIf(fx -> {
            fx.tick();
            if (fx.shouldDiscard()) {
                fx.onDiscard();
                return true;
            }
            return false;
        });
    }

    public static void addFX(AbstractFX fx) {
        FX_LIST.add(fx);
    }

    public static void clearFXs() {
        FX_LIST.clear();
    }
}
