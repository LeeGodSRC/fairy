/*
 * MIT License
 *
 * Copyright (c) 2022 Fairy Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.fairyproject.bukkit.events;

import io.fairyproject.bukkit.FairyBukkitPlatform;
import io.fairyproject.bukkit.listener.RegisterAsListener;
import io.fairyproject.container.InjectableComponent;
import io.fairyproject.container.PostInitialize;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import lombok.RequiredArgsConstructor;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

@InjectableComponent
@RequiredArgsConstructor
@RegisterAsListener
public class GlobalEventListener implements Listener {

    private final Set<Class<? extends Event>> registeredEvents = new HashSet<>();
    private final List<Consumer<Event>> listeners = new ArrayList<>();
    private final Server server;

    @PostInitialize
    public void onPostInitialize() {
        register(Collections.emptyList());
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        Plugin plugin = event.getPlugin();
        URL url = plugin.getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();

        register(Collections.singletonList(url));
    }

    public void addListener(Consumer<Event> listener) {
        this.listeners.add(listener);
    }

    private void onEventFired(Event event) {
        for (Consumer<Event> listener : this.listeners) {
            listener.accept(event);
        }
    }

    private void register(List<URL> urls) {
        Plugin mainPlugin = FairyBukkitPlatform.PLUGIN;
        Listener listener = new Listener() {};

        try {
            for (ClassInfo classInfo : scan(urls)) {
                registerClass(mainPlugin, listener, classInfo);
            }
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    private void registerClass(Plugin mainPlugin, Listener listener, ClassInfo classInfo) throws ClassNotFoundException {
        Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(classInfo.getName());
        EventExecutor eventExecutor = (ignored, event) -> this.onEventFired(event);

        if (!Arrays.stream(eventClass.getDeclaredMethods()).anyMatch(method -> method.getParameterCount() == 0 && method.getName().equals("getHandlers")))
            return;

        if (this.registeredEvents.contains(eventClass))
            return;

        this.registeredEvents.add(eventClass);
        this.server.getPluginManager().registerEvent(eventClass, listener, EventPriority.NORMAL, eventExecutor, mainPlugin);
    }

    private ClassInfoList scan(List<URL> urls) {
        ClassGraph classGraph = new ClassGraph();
        if (!urls.isEmpty())
            classGraph.overrideClasspath(urls.toArray(new URL[0]));

        return classGraph.enableClassInfo().scan()
                .getClassInfo(Event.class.getName())
                .getSubclasses()
                .filter(classInfo -> !classInfo.isAbstract());
    }

}