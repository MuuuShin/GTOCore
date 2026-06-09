package com.gtocore.common.pipe.heat;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.pipenet.IPipeType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum HeatPipeType implements IPipeType<HeatPipeProperties>, StringRepresentable {

    NORMAL;

    public static final ResourceLocation TYPE = GTCEu.id("heat");

    @Override
    public float getThickness() {
        return 0.375F;
    }

    @Override
    public HeatPipeProperties modifyProperties(HeatPipeProperties baseProperties) {
        return baseProperties;
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
