package io.github.noeppi_noeppi.mods.bongo.task;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.github.noeppi_noeppi.libx.render.RenderHelperItem;
import io.github.noeppi_noeppi.mods.bongo.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TaskTypeItem implements TaskType<ItemStack> {

    public static final TaskTypeItem INSTANCE = new TaskTypeItem();

    private TaskTypeItem() {

    }

    @Override
    public Class<ItemStack> getTaskClass() {
        return ItemStack.class;
    }

    @Override
    public String getId() {
        return "bongo.item";
    }

    @Override
    public String getTranslationKey() {
        return "bongo.task.item.name";
    }

    @Override
    public void renderSlot(Minecraft mc, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        AbstractGui.blit(matrixStack, 0, 0, 0, 0, 18, 18, 256, 256);
    }

    @Override
    public void renderSlotContent(Minecraft mc, ItemStack content, MatrixStack matrixStack, IRenderTypeBuffer buffer, boolean bigBongo) {
        RenderHelperItem.renderItemGui(matrixStack, buffer, content, 0, 0, 16, !bigBongo);
    }

    @Override
    public String getTranslatedContentName(ItemStack content) {
        String text = content.getTextComponent().getStringTruncated(16);
        if (text.startsWith("["))
            text = text.substring(1);
        if (text.endsWith("]"))
            text = text.substring(0, text.length() - 1);

        if (content.getCount() > 1)
            text += (" x " + content.getCount());

        return text;
    }

    @Override
    public ITextComponent getContentName(ItemStack content, MinecraftServer server) {
        return content.getTextComponent();
    }

    @Override
    public boolean shouldComplete(ItemStack element, PlayerEntity player, ItemStack compare) {
        if (ItemStack.areItemsEqualIgnoreDurability(element, compare) && element.getCount() <= compare.getCount()) {
            return Util.matchesNBT(element.getTag(), compare.getTag());
        } else {
            return false;
        }
    }

    @Override
    public void consumeItem(ItemStack element, PlayerEntity player) {
        int slot = -1;
        for (ItemStack theStack : player.inventory.mainInventory) {
            if (ItemStack.areItemsEqualIgnoreDurability(element, theStack) && element.getCount() <= theStack.getCount()) {
                if (Util.matchesNBT(element.getTag(), theStack.getTag())) {
                    slot = player.inventory.getSlotFor(theStack);
                }
            }
        }
        if (slot >= 0) {
            player.inventory.decrStackSize(slot, element.getCount());
        }
    }

    @Override
    public Predicate<ItemStack> bongoTooltipStack(ItemStack element) {
        return stack -> ItemStack.areItemsEqual(element, stack);
    }

    @Override
    public CompoundNBT serializeNBT(ItemStack element) {
        return element.write(new CompoundNBT());
    }

    @Override
    public ItemStack deserializeNBT(CompoundNBT nbt) {
        if (!nbt.contains("Count")) {
            nbt.putByte("Count", (byte) 1);
        }
        return ItemStack.read(nbt);
    }

    @Override
    public ItemStack copy(ItemStack element) {
        return element.copy();
    }

    @Override
    public Stream<ItemStack> getAllElements(MinecraftServer server, @Nullable ServerPlayerEntity player) {
        if (player == null) {
            return ForgeRegistries.ITEMS.getValues().stream().flatMap(item -> {
                if (item.getGroup() != null) {
                    NonNullList<ItemStack> nl = NonNullList.create();
                    item.fillItemGroup(item.getGroup(), nl);
                    return nl.stream();
                } else {
                    return Stream.of(new ItemStack(item));
                }
            }).filter(stack -> !stack.isEmpty());
        } else {
            return player.inventory.mainInventory.stream().filter(stack -> !stack.isEmpty());
        }
    }
}
