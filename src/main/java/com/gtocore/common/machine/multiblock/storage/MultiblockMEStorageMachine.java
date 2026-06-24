package com.gtocore.common.machine.multiblock.storage;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;

import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.datasream.data.Data;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MultiblockMEStorageMachine extends MultiblockControllerMachine implements MEStorage, IDropSaveMachine {

    @SaveToDisk
    private final AEKeyMap<AEKey> keyMap = new AEKeyMap<>();
    @SaveToDisk
    private long totalAmount;
    @SaveToDisk
    private long capacity = 100;
    private boolean changes;
    private LazyOptional<MEStorage> capabilityStorage = LazyOptional.of(() -> this);

    private final AEKeyType type;

    public MultiblockMEStorageMachine(MetaMachineBlockEntity holder, AEKeyType type) {
        super(holder);
        this.type = type;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        changes = false;
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        changes = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        capabilityStorage = LazyOptional.of(() -> this);
        changes = false;
    }

    @Override
    public void onUnload() {
        super.onUnload();
        capabilityStorage.invalidate();
        changes = false;
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        if (tag.get("keymap") instanceof ByteArrayTag byteArrayTag) {
            getFieldDataManager().readFieldFromData(Data.readData(byteArrayTag.getAsByteArray()), 0, "keyMap");
        }
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        tag.putByteArray("keymap", getFieldDataManager().writeFieldToData("keyMap").writeToBytes());
    }

    @Override
    public @Nullable <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == Capabilities.STORAGE && side == getFrontFacing()) {
            return capabilityStorage.cast();
        }
        return null;
    }

    @Override
    public Component getDescription() {
        return getDefinition().asItem().getDescription();
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return capacity > totalAmount;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (capacity == 0 || !type.contains(what)) return 0;
        amount = Math.min(capacity - totalAmount, amount);
        if (amount < 1) return 0;
        if (mode == Actionable.MODULATE) {
            keyMap.insert(what, amount);
            saveChanges();
        }
        return amount;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (mode == Actionable.MODULATE) {
            var extract = keyMap.extract(what, amount);
            if (extract > 0) saveChanges();
            return extract;
        } else {
            return Math.min(amount, keyMap.getAmount(what));
        }
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        var map = keyMap;
        if (map.isEmpty()) return;
        out.addAll(map.size(), m -> map.fastForEach(m::addTo));
    }

    private void saveChanges() {
        holder.setChanged();
        long totalAmount = 0;
        for (LongIterator it = keyMap.values().iterator(); it.hasNext();) {
            long amount = it.nextLong();
            totalAmount += amount;
        }
        this.totalAmount = totalAmount;
    }
}
