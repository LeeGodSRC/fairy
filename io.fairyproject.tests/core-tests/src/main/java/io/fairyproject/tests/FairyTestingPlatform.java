package io.fairyproject.tests;

import io.fairyproject.FairyPlatform;
import io.fairyproject.library.Library;
import io.fairyproject.module.ModuleService;
import io.fairyproject.plugin.PluginManager;
import io.fairyproject.task.ITaskScheduler;
import io.fairyproject.task.async.AsyncTaskScheduler;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class FairyTestingPlatform extends FairyPlatform {

    private final Thread thread;
    public FairyTestingPlatform() {
        this.thread = Thread.currentThread();
        if (!PluginManager.isInitialized()) {
            PluginManager.initialize(type -> {
                if (type.getName().startsWith("io.fairytest")) {
                    return "test";
                }
                return null;
            });
        }
    }

    @Override
    public void load() {
        super.load();
        ModuleService.init();
    }

    @Override
    public void enable() {
        super.enable();
        ModuleService.INSTANCE.enable();
    }

    @Override
    public void loadDependencies() {
        // We do not need dependencies here
    }

    @Override
    public File getDataFolder() {
        return new File(".");
    }

    @Override
    public Collection<Library> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public void saveResource(String name, boolean replace) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == this.thread;
    }

    @Override
    public ITaskScheduler createTaskScheduler() {
        return new AsyncTaskScheduler();
    }
}
