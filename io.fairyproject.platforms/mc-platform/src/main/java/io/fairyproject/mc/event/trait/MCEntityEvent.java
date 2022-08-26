package io.fairyproject.mc.event.trait;

import io.fairyproject.mc.MCEntity;
import io.fairyproject.mc.MCWorld;
import org.jetbrains.annotations.NotNull;

public interface MCEntityEvent extends MCWorldEvent {

    @Override
    default @NotNull MCWorld world() {
        return this.entity().getWorld();
    }

    @NotNull MCEntity entity();

}
