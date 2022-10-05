package io.fairyproject.mc.map;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import io.fairyproject.container.*;
import io.fairyproject.container.collection.ContainerObjCollector;
import io.fairyproject.mc.MCPlayer;
import io.fairyproject.mc.map.framebuffers.DirectFramebuffer;
import io.fairyproject.mc.metadata.PlayerOnlineValue;
import io.fairyproject.mc.protocol.MCProtocol;
import io.fairyproject.metadata.MetadataKey;
import io.fairyproject.task.Task;
import io.fairyproject.util.terminable.Terminable;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class MapService {

    public static final MetadataKey<RenderData> MAP_CURRENT = MetadataKey.create("fairy:map", RenderData.class);
    private static final Framebuffer EMPTY_FRAMEBUFFER = new DirectFramebuffer();
    public static final int MAP_ID = 255;

    private final List<MapAdapter> adapters = new ArrayList<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicBoolean activated = new AtomicBoolean();

    @PreInitialize
    public void onPreInitialize() {
        ContainerContext.get().objectCollectorRegistry().add(ContainerObjCollector.create()
                .withFilter(ContainerObjCollector.inherits(MapAdapter.class))
                .withAddHandler(ContainerObjCollector.warpInstance(MapAdapter.class, this::registerAdapter))
                .withRemoveHandler(ContainerObjCollector.warpInstance(MapAdapter.class, this::unregisterAdapter))
        );
    }

    @PostInitialize
    public void onPostInitialize() {
        this.activate();
    }

    public void activate() {
        if (this.activated.compareAndSet(false, true)) {
            Containers.bindWith(this, Task.asyncRepeated(this::tick, this.getUpdateTick()));
        }
    }

    private void tick(Terminable terminable) {
        for (MCPlayer player : MCPlayer.all()) {
            RenderData previous = player.metadata().getOrDefault(MAP_CURRENT, null);
            Framebuffer framebuffer = this.render(player);
            if (framebuffer == null)
                framebuffer = EMPTY_FRAMEBUFFER;

            final RenderData current = framebuffer.preparePacket(MAP_ID);
            if (Objects.equals(previous, current))
                continue;

            player.metadata().put(MAP_CURRENT, PlayerOnlineValue.create(current, player));
            MCProtocol.sendPacket(player, new WrapperPlayServerMapData(
                    current.id(),
                    0,
                    false,
                    current.icons(),
                    current.colors(),
                    current.x(),
                    current.y(),
                    current.width(),
                    current.height()
            ));
        }

        this.lock.readLock().lock();
        try {
            if (this.adapters.isEmpty()) {
                // No adapter registered at the moment
                terminable.closeAndReportException();
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Nullable
    private Framebuffer render(MCPlayer player) {
        Framebuffer retVal = null;

        this.lock.readLock().lock();
        try {
            for (MapAdapter adapter : this.getSortedAdapters()) {
                retVal = adapter.render(player);
                if (retVal != null) {
                    break;
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }

        return retVal;
    }

    private int getUpdateTick() {
        int tick = 2;
        this.lock.readLock().lock();
        try {
            for (MapAdapter adapter : this.getSortedAdapters()) {
                int adapterTick = adapter.ticks();
                if (adapterTick != -1) {
                    tick = adapterTick;
                    break;
                }
            }
        } finally {
            this.lock.readLock().unlock();
        }

        return tick;
    }

    private List<MapAdapter> getSortedAdapters() {
        List<MapAdapter> actionbarAdapters;

        this.lock.readLock().lock();
        try {
            actionbarAdapters = new ArrayList<>(this.adapters);
        } finally {
            this.lock.readLock().unlock();
        }

        actionbarAdapters.sort(Collections.reverseOrder(Comparator.comparingInt(MapAdapter::priority)));
        return actionbarAdapters;
    }

    public void registerAdapter(MapAdapter actionbarAdapter) {
        this.lock.writeLock().lock();
        try {
            this.adapters.add(actionbarAdapter);
        } finally {
            this.lock.writeLock().unlock();
        }

        this.activate();
    }

    public void unregisterAdapter(MapAdapter actionbarAdapter) {
        this.lock.writeLock().lock();
        try {
            this.adapters.remove(actionbarAdapter);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

}
