package io.github.noeppi_noeppi.mods.bongo.task;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import io.github.noeppi_noeppi.mods.bongo.util.Highlight;
import io.github.noeppi_noeppi.mods.bongo.util.ItemRenderUtil;
import io.github.noeppi_noeppi.mods.bongo.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.moddingx.libx.codec.MoreCodecs;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.stream.Stream;

public class TaskTypeItem implements TaskType<ItemStack> {

    public static final TaskTypeItem INSTANCE = new TaskTypeItem();

    private TaskTypeItem() {

    }

    @Override
    public String id() {
        return "bongo.item";
    }

    @Override
    public Class<ItemStack> taskClass() {
        return ItemStack.class;
    }

    @Override
    public MapCodec<ItemStack> codec() {
        return MoreCodecs.SAFE_ITEM_STACK.fieldOf("value");
    }

    @Override
    public Component name() {
        return Component.translatable("bongo.task.item.name");
    }

    @Override
    public Component contentName(ItemStack element, @Nullable MinecraftServer server) {
        return element.getHoverName();
    }

    @Override
    public Comparator<ItemStack> order() {
        return Comparator.<ItemStack, ResourceLocation>comparing(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()), Util.COMPARE_RESOURCE).thenComparingInt(ItemStack::getCount);
    }

    @Override
    public Stream<ItemStack> listElements(MinecraftServer server, @Nullable ServerPlayer player) {
        if (player == null) {
            return ForgeRegistries.ITEMS.getValues().stream().flatMap(item -> {
                if (item.getItemCategory() != null) {
                    NonNullList<ItemStack> nl = NonNullList.create();
                    item.fillItemCategory(CreativeModeTab.TAB_SEARCH, nl);
                    return nl.stream();
                } else {
                    return Stream.of(new ItemStack(item));
                }
            }).filter(stack -> !stack.isEmpty());
        } else {
            return player.getInventory().items.stream().filter(stack -> !stack.isEmpty());
        }
    }

    @Override
    public boolean shouldComplete(ServerPlayer player, ItemStack element, ItemStack compare) {
        if (ItemStack.isSameIgnoreDurability(element, compare) && element.getCount() <= compare.getCount()) {
            return Util.matchesNBT(element.getTag(), compare.getTag());
        } else {
            return false;
        }
    }

    @Override
    public void consume(ServerPlayer player, ItemStack element, ItemStack found) {
        Util.removeItems(player, element.getCount(), stack -> ItemStack.isSameIgnoreDurability(element, stack) && Util.matchesNBT(element.getTag(), stack.getTag()));
    }

    @Override
    public Stream<Highlight<?>> highlight(ItemStack element) {
        return Stream.of(new Highlight.Item(element));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public FormattedCharSequence renderDisplayName(Minecraft mc, ItemStack element) {
        FormattedCharSequence name = TaskType.super.renderDisplayName(mc, element);
        if (element.getCount() != 1) {
            return FormattedCharSequence.composite(name, FormattedCharSequence.forward(" x " + element.getCount(), Style.EMPTY));
        } else {
            return name;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderSlot(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer) {
        GuiComponent.blit(poseStack, 0, 0, 0, 0, 18, 18, 256, 256);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderSlotContent(Minecraft mc, ItemStack element, PoseStack poseStack, MultiBufferSource buffer, boolean bigBongo) {
        ItemRenderUtil.renderItem(poseStack, buffer, element, !bigBongo);
    }
}
