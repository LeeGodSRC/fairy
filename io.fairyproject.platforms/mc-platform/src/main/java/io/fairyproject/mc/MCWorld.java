package io.fairyproject.mc;

import io.fairyproject.mc.util.AudienceProxy;
import io.fairyproject.mc.util.Pos;
import io.fairyproject.mc.world.MCChunk;
import io.fairyproject.metadata.MetadataMap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface MCWorld extends AudienceProxy {

    static <T> MCWorld from(T world) {
        return Companion.BRIDGE.from(world);
    }

    static MCWorld getByName(String name) {
        return Companion.BRIDGE.getByName(name);
    }

    static List<MCWorld> all() {
        return Companion.BRIDGE.all();
    }

    <T> T as(Class<T> worldClass);

    int getMaxY();

    int getMaxSectionY();

    String name();

    MetadataMap metadata();

    List<MCPlayer> players();

    // chunks
    List<MCChunk> chunks();

    MCChunk getChunkAt(int x, int z);

    @UtilityClass
    class Companion {
        public Bridge BRIDGE;
    }

    interface Bridge {

        MCWorld from(Object world);

        MCWorld getByName(String name);

        List<MCWorld> all();

    }

}
