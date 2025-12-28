package ru.realite.core.impl;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleManager;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.ModuleState;
import ru.realite.core.api.Platform;
import ru.realite.core.api.StorageService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Менеджер модулей:
 * - регистрирует
 * - сортирует по зависимостям
 * - загружает/включает/выключает
 */
public final class ModuleManagerImpl implements ModuleManager {

    private final CoreApi core;
    private final Platform log;

    private final Map<ModuleId, Module> modulesById = new LinkedHashMap<>();
    private final Map<ModuleId, ModuleState> states = new HashMap<>();
    private final Map<ModuleId, ModuleContext> contexts = new HashMap<>();
    private final List<ModuleId> loadOrder = new ArrayList<>();
    private final List<ModuleId> enabledOrder = new ArrayList<>();

    public ModuleManagerImpl(CoreApi core) {
        this.core = Objects.requireNonNull(core, "core");
        this.log = core.platform();
    }

    void registerFailed(ModuleMetadata metadata, String reason, Exception e) {
        Objects.requireNonNull(metadata, "metadata");
        ModuleId id = Objects.requireNonNull(metadata.id(), "metadata.id()");
        modulesById.putIfAbsent(id, new FailedModule(metadata));
        markFailed(id, reason, e);
    }

    @Override
    public void register(Module module) {
        Objects.requireNonNull(module, "module");
        ModuleMetadata metadata = Objects.requireNonNull(module.metadata(), "module.metadata()");
        ModuleId id = Objects.requireNonNull(metadata.id(), "module.metadata().id()");

        Module prev = modulesById.putIfAbsent(id, module);
        if (prev != null) {
            throw new IllegalStateException("Module already registered: " + id
                    + " (existing=" + prev.getClass().getName()
                    + ", new=" + module.getClass().getName() + ")");
        }
        states.put(id, ModuleState.NEW);
    }

    @Override
    public void loadAll() {
        ModuleGraph graph = ModuleGraph.fromModules(modulesById.values());
        loadOrder.clear();

        for (ModuleId id : graph.registeredModules()) {
            Set<ModuleId> missing = graph.missingDependencies(id);
            if (!missing.isEmpty()) {
                markFailed(id, "Missing dependencies: " + missing);
            }
        }

        List<ModuleId> ordered;
        try {
            ordered = graph.topologicalSort();
        } catch (ModuleGraph.ModuleCycleException e) {
            Set<ModuleId> failed = graph.dependentsOf(new LinkedHashSet<>(e.cycle()));
            for (ModuleId id : failed) {
                markFailed(id, "Failed due to dependency cycle");
            }
            log.error(e.getMessage(), e);
            throw e;
        }

        for (ModuleId id : ordered) {
            ModuleState currentState = state(id);
            if (currentState == ModuleState.FAILED) {
                continue;
            }
            if (currentState == ModuleState.LOADED
                    || currentState == ModuleState.ENABLED
                    || currentState == ModuleState.DISABLED) {
                loadOrder.add(id);
                continue;
            }
            if (hasFailedDependencies(id, graph)) {
                markFailed(id, "Dependency failed");
                continue;
            }

            Module module = modulesById.get(id);
            ModuleContext ctx = contextFor(module);
            try {
                log.info("Loading module: " + id);
                module.onLoad(ctx);
                states.put(id, ModuleState.LOADED);
                loadOrder.add(id);
            } catch (Exception e) {
                markFailed(id, "Failed to load module", e);
            }
        }
    }

    @Override
    public void enableAll() {
        loadAll();

        ModuleGraph graph = ModuleGraph.fromModules(modulesById.values());
        List<ModuleId> ordered = graph.topologicalSort();

        for (ModuleId id : ordered) {
            if (state(id) == ModuleState.FAILED) {
                continue;
            }
            if (state(id) == ModuleState.ENABLED) {
                continue;
            }
            if (hasFailedDependencies(id, graph)) {
                markFailed(id, "Dependency failed");
                continue;
            }
            if (!dependenciesEnabled(id, graph)) {
                markFailed(id, "Dependency not enabled");
                continue;
            }
            Module module = modulesById.get(id);
            ModuleContext ctx = contextFor(module);
            try {
                log.info("Enabling module: " + id);
                module.onEnable(ctx);
                states.put(id, ModuleState.ENABLED);
                enabledOrder.add(id);
            } catch (Exception e) {
                markFailed(id, "Failed to enable module", e);
            }
        }
    }

    @Override
    public void disableAll() {
        for (int i = enabledOrder.size() - 1; i >= 0; i--) {
            ModuleId id = enabledOrder.get(i);
            disableModule(id);
        }
        enabledOrder.clear();
        StorageService storageService = core.services().get(StorageService.class);
        if (storageService != null) {
            storageService.shutdown();
        }
    }

    @Override
    public void enable(ModuleId id) {
        Objects.requireNonNull(id, "id");
        loadAll();
        if (!modulesById.containsKey(id)) {
            throw new IllegalArgumentException("Module not registered: " + id);
        }
        ModuleGraph graph = ModuleGraph.fromModules(modulesById.values());
        Set<ModuleId> required = new LinkedHashSet<>(graph.dependenciesOf(id));
        required.add(id);
        for (ModuleId moduleId : graph.topologicalSort()) {
            if (!required.contains(moduleId)) {
                continue;
            }
            if (state(moduleId) == ModuleState.ENABLED) {
                continue;
            }
            if (hasFailedDependencies(moduleId, graph)) {
                markFailed(moduleId, "Dependency failed");
                continue;
            }
            if (!dependenciesEnabled(moduleId, graph)) {
                markFailed(moduleId, "Dependency not enabled");
                continue;
            }
            Module module = modulesById.get(moduleId);
            ModuleContext ctx = contextFor(module);
            try {
                log.info("Enabling module: " + moduleId);
                module.onEnable(ctx);
                states.put(moduleId, ModuleState.ENABLED);
                enabledOrder.add(moduleId);
            } catch (Exception e) {
                markFailed(moduleId, "Failed to enable module", e);
            }
        }
    }

    @Override
    public void disable(ModuleId id) {
        Objects.requireNonNull(id, "id");
        if (!modulesById.containsKey(id)) {
            throw new IllegalArgumentException("Module not registered: " + id);
        }
        ModuleGraph graph = ModuleGraph.fromModules(modulesById.values());
        Set<ModuleId> toDisable = graph.dependentsOf(Set.of(id));
        List<ModuleId> ordered = graph.topologicalSort();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            ModuleId current = ordered.get(i);
            if (toDisable.contains(current)) {
                disableModule(current);
                enabledOrder.remove(current);
            }
        }
    }

    @Override
    public ModuleState state(ModuleId id) {
        Objects.requireNonNull(id, "id");
        ModuleState state = states.get(id);
        if (state == null) {
            throw new IllegalArgumentException("Module not registered: " + id);
        }
        return state;
    }

    @Override
    public Collection<Module> modules() {
        return Collections.unmodifiableCollection(modulesById.values());
    }

    private ModuleContext contextFor(Module module) {
        ModuleMetadata metadata = module.metadata();
        return contexts.computeIfAbsent(metadata.id(), id -> {
            Path dataFolder = core.dataDirectory()
                    .resolve("modules")
                    .resolve(id.value());
            return new ModuleContextImpl(core, metadata, dataFolder);
        });
    }

    private boolean hasFailedDependencies(ModuleId id, ModuleGraph graph) {
        for (ModuleId dep : graph.dependenciesOf(id)) {
            if (states.getOrDefault(dep, ModuleState.NEW) == ModuleState.FAILED) {
                return true;
            }
        }
        return false;
    }

    private boolean dependenciesEnabled(ModuleId id, ModuleGraph graph) {
        for (ModuleId dep : graph.dependenciesOf(id)) {
            if (states.getOrDefault(dep, ModuleState.NEW) != ModuleState.ENABLED) {
                return false;
            }
        }
        return true;
    }

    private void disableModule(ModuleId id) {
        ModuleState state = states.get(id);
        if (state != ModuleState.ENABLED) {
            return;
        }
        Module module = modulesById.get(id);
        ModuleContext ctx = contextFor(module);
        try {
            log.info("Disabling module: " + id);
            module.onDisable(ctx);
            states.put(id, ModuleState.DISABLED);
        } catch (Exception e) {
            markFailed(id, "Failed to disable module", e);
        }
    }

    private void markFailed(ModuleId id, String reason) {
        log.error("Module failed: " + id + ". " + reason, null);
        states.put(id, ModuleState.FAILED);
    }

    private void markFailed(ModuleId id, String reason, Exception e) {
        log.error("Module failed: " + id + ". " + reason, e);
        states.put(id, ModuleState.FAILED);
    }

    private static final class FailedModule implements Module {
        private final ModuleMetadata metadata;

        private FailedModule(ModuleMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public ModuleMetadata metadata() {
            return metadata;
        }

        @Override
        public void onLoad(ModuleContext ctx) {
            // no-op
        }

        @Override
        public void onEnable(ModuleContext ctx) {
            // no-op
        }

        @Override
        public void onDisable(ModuleContext ctx) {
            // no-op
        }
    }
}
