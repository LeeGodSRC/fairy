package io.fairyproject.mc.map.framebuffers;

import com.github.retrooper.packetevents.protocol.map.MapIcon;
import io.fairyproject.mc.map.Framebuffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseFramebuffer implements Framebuffer {

    private final List<MapIcon> icons = new ArrayList<>();

    @Override
    public Collection<MapIcon> icons() {
        return this.icons;
    }

    public void addIcon(MapIcon icon) {
        this.icons.add(icon);
    }
}
