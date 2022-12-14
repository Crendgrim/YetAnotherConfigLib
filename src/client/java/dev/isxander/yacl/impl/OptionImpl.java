package dev.isxander.yacl.impl;

import com.google.common.collect.ImmutableSet;
import dev.isxander.yacl.api.Binding;
import dev.isxander.yacl.api.Controller;
import dev.isxander.yacl.api.Option;
import dev.isxander.yacl.api.OptionFlag;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApiStatus.Internal
public final class OptionImpl<T> implements Option<T> {
    private final Text name;
    private Text tooltip;
    private final Controller<T> controller;
    private final Binding<T> binding;
    private boolean available;

    private final ImmutableSet<OptionFlag> flags;

    private final Class<T> typeClass;

    private T pendingValue;

    private final List<BiConsumer<Option<T>, T>> listeners;

    public OptionImpl(
            @NotNull Text name,
            @Nullable Function<T, Text> tooltipGetter,
            @NotNull Function<Option<T>, Controller<T>> controlGetter,
            @NotNull Binding<T> binding,
            boolean available,
            ImmutableSet<OptionFlag> flags,
            @NotNull Class<T> typeClass,
            @NotNull Collection<BiConsumer<Option<T>, T>> listeners
    ) {
        this.name = name;
        this.binding = binding;
        this.available = available;
        this.flags = flags;
        this.typeClass = typeClass;
        this.listeners = new ArrayList<>(listeners);
        this.controller = controlGetter.apply(this);

        addListener((opt, pending) -> tooltip = tooltipGetter.apply(pending));
        requestSet(binding().getValue());
    }

    @Override
    public @NotNull Text name() {
        return name;
    }

    @Override
    public @NotNull Text tooltip() {
        return tooltip;
    }

    @Override
    public @NotNull Controller<T> controller() {
        return controller;
    }

    @Override
    public @NotNull Binding<T> binding() {
        return binding;
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public @NotNull Class<T> typeClass() {
        return typeClass;
    }

    @Override
    public @NotNull ImmutableSet<OptionFlag> flags() {
        return flags;
    }

    @Override
    public boolean changed() {
        return !binding().getValue().equals(pendingValue);
    }

    @Override
    public @NotNull T pendingValue() {
        return pendingValue;
    }

    @Override
    public void requestSet(T value) {
        pendingValue = value;
        listeners.forEach(listener -> listener.accept(this, pendingValue));
    }

    @Override
    public boolean applyValue() {
        if (changed()) {
            binding().setValue(pendingValue);
            return true;
        }
        return false;
    }

    @Override
    public void forgetPendingValue() {
        requestSet(binding().getValue());
    }

    @Override
    public void requestSetDefault() {
        requestSet(binding().defaultValue());
    }

    @Override
    public boolean isPendingValueDefault() {
        return binding().defaultValue().equals(pendingValue());
    }

    @Override
    public void addListener(BiConsumer<Option<T>, T> changedListener) {
        this.listeners.add(changedListener);
    }

    @ApiStatus.Internal
    public static class BuilderImpl<T> implements Option.Builder<T> {
        private Text name = Text.literal("Name not specified!").formatted(Formatting.RED);

        private final List<Function<T, Text>> tooltipGetters = new ArrayList<>();

        private Function<Option<T>, Controller<T>> controlGetter;

        private Binding<T> binding;

        private boolean available = true;

        private boolean instant = false;

        private final Set<OptionFlag> flags = new HashSet<>();

        private final Class<T> typeClass;

        private final List<BiConsumer<Option<T>, T>> listeners = new ArrayList<>();

        public BuilderImpl(Class<T> typeClass) {
            this.typeClass = typeClass;
        }

        @Override
        public Option.Builder<T> name(@NotNull Text name) {
            Validate.notNull(name, "`name` cannot be null");

            this.name = name;
            return this;
        }

        @Override
        @SafeVarargs
        public final Option.Builder<T> tooltip(@NotNull Function<T, Text>... tooltipGetter) {
            Validate.notNull(tooltipGetter, "`tooltipGetter` cannot be null");

            this.tooltipGetters.addAll(List.of(tooltipGetter));
            return this;
        }

        @Override
        public Option.Builder<T> tooltip(@NotNull Text... tooltips) {
            Validate.notNull(tooltips, "`tooltips` cannot be empty");

            this.tooltipGetters.addAll(Stream.of(tooltips).map(text -> (Function<T, Text>) t -> text).toList());
            return this;
        }

        @Override
        public Option.Builder<T> controller(@NotNull Function<Option<T>, Controller<T>> control) {
            Validate.notNull(control, "`control` cannot be null");

            this.controlGetter = control;
            return this;
        }

        @Override
        public Option.Builder<T> binding(@NotNull Binding<T> binding) {
            Validate.notNull(binding, "`binding` cannot be null");

            this.binding = binding;
            return this;
        }

        @Override
        public Option.Builder<T> binding(@NotNull T def, @NotNull Supplier<@NotNull T> getter, @NotNull Consumer<@NotNull T> setter) {
            Validate.notNull(def, "`def` must not be null");
            Validate.notNull(getter, "`getter` must not be null");
            Validate.notNull(setter, "`setter` must not be null");

            this.binding = Binding.generic(def, getter, setter);
            return this;
        }

        @Override
        public Option.Builder<T> available(boolean available) {
            this.available = available;
            return this;
        }

        @Override
        public Option.Builder<T> flag(@NotNull OptionFlag... flag) {
            Validate.notNull(flag, "`flag` must not be null");

            this.flags.addAll(Arrays.asList(flag));
            return this;
        }

        @Override
        public Option.Builder<T> flags(@NotNull Collection<OptionFlag> flags) {
            Validate.notNull(flags, "`flags` must not be null");

            this.flags.addAll(flags);
            return this;
        }

        @Override
        public Option.Builder<T> instant(boolean instant) {
            this.instant = instant;
            return this;
        }

        @Override
        public Option.Builder<T> listener(@NotNull BiConsumer<Option<T>, T> listener) {
            this.listeners.add(listener);
            return this;
        }

        @Override
        public Option.Builder<T> listeners(@NotNull Collection<BiConsumer<Option<T>, T>> listeners) {
            this.listeners.addAll(listeners);
            return this;
        }

        @Override
        public Option<T> build() {
            Validate.notNull(controlGetter, "`control` must not be null when building `Option`");
            Validate.notNull(binding, "`binding` must not be null when building `Option`");
            Validate.isTrue(!instant || flags.isEmpty(), "instant application does not support option flags");

            Function<T, Text> concatenatedTooltipGetter = value -> {
                MutableText concatenatedTooltip = Text.empty();
                boolean first = true;
                for (Function<T, Text> line : tooltipGetters) {
                    if (!first) concatenatedTooltip.append("\n");
                    first = false;

                    concatenatedTooltip.append(line.apply(value));
                }

                return concatenatedTooltip;
            };

            if (instant) {
                listeners.add((opt, pendingValue) -> opt.applyValue());
            }

            return new OptionImpl<>(name, concatenatedTooltipGetter, controlGetter, binding, available, ImmutableSet.copyOf(flags), typeClass, listeners);
        }
    }
}
