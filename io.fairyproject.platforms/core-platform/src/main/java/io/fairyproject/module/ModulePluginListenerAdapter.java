package io.fairyproject.module;

import com.google.gson.JsonObject;
import io.fairyproject.Debug;
import io.fairyproject.Fairy;
import io.fairyproject.container.ContainerContext;
import io.fairyproject.container.Containers;
import io.fairyproject.plugin.Plugin;
import io.fairyproject.plugin.PluginAction;
import io.fairyproject.plugin.PluginDescription;
import io.fairyproject.plugin.PluginListenerAdapter;
import io.fairyproject.util.exceptionally.ThrowingSupplier;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class ModulePluginListenerAdapter implements PluginListenerAdapter {

    private static final MessageDigest DIGEST = ThrowingSupplier.sneaky(() -> MessageDigest.getInstance("SHA-256")).get();
    private static final Logger LOGGER = LogManager.getLogger(ModulePluginListenerAdapter.class);
    private final ModuleService moduleService;

    @Override
    public void onPluginPreLoaded(ClassLoader classLoader,
                                  PluginDescription pluginDescription,
                                  PluginAction action,
                                  CompletableFuture<Plugin> pluginCompletableFuture) {
        // We will get all the paths first for relocation.
        ModuleService.ModuleDataList modules = new ModuleService.ModuleDataList();
        this.downloadModules(pluginDescription, modules);

        if (Debug.UNIT_TEST) {
            this.loadUnitTestModules(pluginDescription, classLoader, modules);
        }

        Collections.reverse(modules);
        this.loadLibrariesAndRemap(modules, action, pluginDescription);

        this.moduleService.pendingProcessBatch.runOrQueue(pluginDescription.getName(), () -> {
            // Then push all paths
            this.loadAllModules(modules, pluginDescription, pluginCompletableFuture);
        });
    }

    private void loadAllModules(ModuleService.ModuleDataList modules, PluginDescription pluginDescription, CompletableFuture<Plugin> pluginCompletableFuture) {
        modules.forEach(moduleData -> {
            final Module module = this.moduleService.load(moduleData, pluginDescription, pluginCompletableFuture);

            if (module == null) {
                ContainerContext.warn("[>>Adapter>>] [Critical] Failed to find module data @ %s (%s)",
                        moduleData.getName(), moduleData.getPath().toString());
                return;
            }

            module.addRef(); // add reference
            pluginCompletableFuture.whenComplete((plugin, throwable) -> {
                if (throwable == null) {
                    LOGGER.info("Loaded module " + module.getName() + " into plugin " + plugin.getName());
                    plugin.getLoadedModules().add(module);
                } else {
                    LOGGER.warn("Failed to load module " + module.getName() + " into plugin " + plugin.getName(), throwable);
                }
            });
        });
    }

    private void loadLibrariesAndRemap(ModuleService.ModuleDataList modules, PluginAction action, PluginDescription pluginDescription) {
        // Relocation entries from all included modules
        final Path[] relocationEntries = modules.stream()
                .map(ModuleService.ModuleData::getPath)
                .toArray(Path[]::new);

        modules.forEach(moduleData -> {
                    try {
                        JsonObject jsonObject = this.moduleService.readModuleData(moduleData.getPath());
                        if (jsonObject == null) {
                            ContainerContext.warn("[>>Adapter>>] [Critical] Failed to find module data @ %s (%s)",
                                    moduleData.getName(), moduleData.getPath().toString());
                            return;
                        }

                        this.moduleService.loadLibraries(jsonObject);
                        this.tryRelocateJar(modules, moduleData, jsonObject, pluginDescription, action, relocationEntries);

                        ContainerContext.log("[>>Remapper>>] Successfully loaded %s (from: %s)", moduleData.getName(), moduleData.getShadedPath().toString());
                        Fairy.getPlatform().getClassLoaderAccess().addPath(moduleData.getShadedPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void tryRelocateJar(ModuleService.ModuleDataList modules,
                                ModuleService.ModuleData moduleData,
                                JsonObject jsonObject,
                                PluginDescription pluginDescription,
                                PluginAction action,
                                Path[] relocationEntries) throws IOException {
        // Relocation after depended on modules are loaded
        final String fullFileName = moduleData.getPath().getFileName().toString();
        final String fileName = FilenameUtils.getBaseName(fullFileName);

        final Path directory = Files.createDirectories(action.getDataFolder().resolve("modules/" + moduleData.getName() + "/" + moduleData.getVersion()));

        if (!Debug.UNIT_TEST) {
            Path shadedPath = directory.resolve(fullFileName);
            Path sha256Path = directory.resolve(fileName + ".sha256");
            byte[] hashedCurrent = null;

            boolean shouldRemap = false;
            // If shaded jar doesn't exist, should relocate
            if (!Files.exists(shadedPath)) {
                shouldRemap = true;
            } else if (Files.exists(sha256Path)) {
                // If sha256 hash of the original file isn't match, should remap
                final byte[] hashedBefore = Files.readAllBytes(sha256Path);
                hashedCurrent = DIGEST.digest(Files.readAllBytes(moduleData.getPath()));

                if (!Arrays.equals(hashedBefore, hashedCurrent)) {
                    shouldRemap = true;
                }
            } else {
                // If the sha256 hash cache doesn't exist, should remap
                shouldRemap = true;
            }

            if (shouldRemap) {
                if (hashedCurrent == null) {
                    hashedCurrent = DIGEST.digest(Files.readAllBytes(moduleData.getPath()));
                }

                this.moduleService.remap(moduleData.getPath(), shadedPath, pluginDescription, this.moduleService.loadDependModulesData(jsonObject, moduleData.getName(), modules), relocationEntries);
                Files.write(sha256Path, hashedCurrent, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }

            moduleData.setShadedPath(shadedPath);
        } else {
            moduleData.setShadedPath(moduleData.getPath());
        }
    }

    private void loadUnitTestModules(PluginDescription pluginDescription, ClassLoader classLoader, ModuleService.ModuleDataList modules) {
        try {
            final Class<?> moduleClass = Class.forName("MODULE", true, classLoader);
            final Field field = moduleClass.getDeclaredField("ALL");

            final List<String> all = (List<String>) field.get(null);
            for (String tag : all) {
                String[] split = tag.split(":");
//                                String groupId = split[0]; // TODO
                String artifactId = split[1];
                String version = split[2];

                this.moduleService.downloadModules(artifactId, version, modules, false);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load modules for " + pluginDescription.getName(), ex);
        }
    }

    private void downloadModules(PluginDescription pluginDescription, ModuleService.ModuleDataList modules) {
        pluginDescription.getModules()
                .forEach(pair -> {
                    String name = pair.getKey();
                    String version = pair.getValue();

                    this.moduleService.downloadModules(name, version, modules, false);
                });
    }

    @Override
    public void onPluginDisable(Plugin plugin) {
        if (this.moduleService.pendingProcessBatch.remove(plugin.getName())) {
            return;
        }
        final ModuleService moduleService = Containers.get(ModuleService.class);
        plugin.getLoadedModules().forEach(module -> {
            if (module.removeRef() <= 0) {
                moduleService.unregister(module);
            }
        });
    }

    @Override
    public int priority() {
        return ModuleService.PLUGIN_LISTENER_PRIORITY;
    }
}
