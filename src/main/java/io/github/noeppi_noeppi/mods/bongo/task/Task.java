package io.github.noeppi_noeppi.mods.bongo.task;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.noeppi_noeppi.mods.bongo.BongoMod;
import io.github.noeppi_noeppi.mods.bongo.util.Highlight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Unit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.moddingx.libx.codec.CodecHelper;
import org.moddingx.libx.codec.MoreCodecs;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

public class Task {

    public static Task EMPTY = new Task(TaskTypeEmpty.INSTANCE, Unit.INSTANCE);

    @SuppressWarnings("unchecked")
    public static final Codec<Task> CODEC = MoreCodecs.mapDispatch(
            TaskTypes.CODEC.fieldOf("type"),
            TaskTypes::getCodec,
            task -> Pair.of(task.type, task.element),
            (type, element) -> CodecHelper.doesNotThrow(() -> new Task((TaskType<Object>) type, element))
    );

    private final TaskType<?> type;
    private final Object element;

    public <T> Task(TaskType<T> type, T element) {
        this.type = type;
        this.element = element;
        validateElementType();
    }

    public TaskType<?> getType() {
        return type;
    }

    public Component typeName() {
        return this.type.name();
    }
    
    public Component contentName(@Nullable MinecraftServer server) {
        //noinspection unchecked
        return ((TaskType<Object>) this.type).contentName(this.element, server);
    }
    
    public void sync(MinecraftServer server, @Nullable ServerPlayer target) {
        //noinspection unchecked
        ((TaskType<Object>) this.type).sync(this.element, server, target);
    }
    
    public <T> boolean shouldComplete(TaskType<T> type, ServerPlayer player, T compare) {
        if (this.type == type) {
            //noinspection unchecked
            return ((TaskType<Object>) this.type).shouldComplete(player, this.element, compare);
        } else {
            return false;
        }
    }
    
    public <T> void consume(TaskType<T> type, ServerPlayer player, T found) {
        if (this.type == type) {
            //noinspection unchecked
            ((TaskType<Object>) this.type).consume(player, this.element, found);
        }
    }
    
    public Stream<Highlight<?>> highlight() {
        //noinspection unchecked
        return ((TaskType<Object>) this.type).highlight(this.element);
    }
    
    public void invalidate() {
        //noinspection unchecked
        ((TaskType<Object>) this.type).invalidate(this.element);
    }

    @OnlyIn(Dist.CLIENT)
    public FormattedCharSequence renderDisplayName(Minecraft mc) {
        //noinspection unchecked
        return ((TaskType<Object>) this.type).renderDisplayName(mc, this.element);
    }
    
    @OnlyIn(Dist.CLIENT)
    public void renderSlot(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer) {
        this.type.renderSlot(mc, poseStack, buffer);
    }
    
    @OnlyIn(Dist.CLIENT)
    public void renderSlotContent(Minecraft mc, PoseStack poseStack, MultiBufferSource buffer, boolean bigBongo) {
        //noinspection unchecked
        ((TaskType<Object>) this.type).renderSlotContent(mc, this.element, poseStack, buffer, bigBongo);
    }
    
    public void validate(MinecraftServer server) {
        try {
            //noinspection unchecked
            ((TaskType<Object>) type).validate(element, server);
        } catch (Exception e) {
            BongoMod.logger.error("Failed to validate task of type {}: {}", type.id(), e.getMessage());
        }
    }
    
    public <T> Optional<T> getElement(TaskType<T> type) {
        if (this.type == type) {
            //noinspection unchecked
            return Optional.of((T) element);
        } else {
            return Optional.empty();
        }
    }
    
    private void validateElementType() {
        if (!type.taskClass().isAssignableFrom(element.getClass())) {
            throw new IllegalStateException("Can't create task of type " + type.id() + " with element of type " + element.getClass());
        }
    }
}
