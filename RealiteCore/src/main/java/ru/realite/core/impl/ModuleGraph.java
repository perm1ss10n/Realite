package ru.realite.core.impl;

import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Граф зависимостей модулей и топологическая сортировка.
 */
public final class ModuleGraph {

    private final Map<ModuleId, ModuleMetadata> metadataById;
    private final Map<ModuleId, Set<ModuleId>> dependencies;
    private final Map<ModuleId, Set<ModuleId>> dependents;

    private ModuleGraph(
            Map<ModuleId, ModuleMetadata> metadataById,
            Map<ModuleId, Set<ModuleId>> dependencies,
            Map<ModuleId, Set<ModuleId>> dependents
    ) {
        this.metadataById = metadataById;
        this.dependencies = dependencies;
        this.dependents = dependents;
    }

    public static ModuleGraph fromModules(Collection<Module> modules) {
        Objects.requireNonNull(modules, "modules");
        Map<ModuleId, ModuleMetadata> metadataById = new LinkedHashMap<>();
        for (Module module : modules) {
            ModuleMetadata metadata = Objects.requireNonNull(module.metadata(), "module.metadata()");
            metadataById.put(metadata.id(), metadata);
        }

        Map<ModuleId, Set<ModuleId>> dependencies = new LinkedHashMap<>();
        Map<ModuleId, Set<ModuleId>> dependents = new LinkedHashMap<>();

        for (ModuleMetadata metadata : metadataById.values()) {
            Set<ModuleId> deps = new LinkedHashSet<>(metadata.dependencies());
            dependencies.put(metadata.id(), deps);
            dependents.putIfAbsent(metadata.id(), new LinkedHashSet<>());
        }

        for (Map.Entry<ModuleId, Set<ModuleId>> entry : dependencies.entrySet()) {
            ModuleId moduleId = entry.getKey();
            for (ModuleId dep : entry.getValue()) {
                if (metadataById.containsKey(dep)) {
                    dependents.computeIfAbsent(dep, ignored -> new LinkedHashSet<>()).add(moduleId);
                }
            }
        }

        return new ModuleGraph(metadataById, dependencies, dependents);
    }

    public Set<ModuleId> registeredModules() {
        return metadataById.keySet();
    }

    public Set<ModuleId> missingDependencies(ModuleId id) {
        Set<ModuleId> missing = new LinkedHashSet<>();
        Set<ModuleId> deps = dependencies.getOrDefault(id, Set.of());
        for (ModuleId dep : deps) {
            if (!metadataById.containsKey(dep)) {
                missing.add(dep);
            }
        }
        return missing;
    }

    public Set<ModuleId> dependenciesOf(ModuleId id) {
        Set<ModuleId> visited = new LinkedHashSet<>();
        Deque<ModuleId> stack = new ArrayDeque<>();
        stack.add(id);
        while (!stack.isEmpty()) {
            ModuleId current = stack.removeLast();
            for (ModuleId dep : dependencies.getOrDefault(current, Set.of())) {
                if (!metadataById.containsKey(dep)) {
                    continue;
                }
                if (visited.add(dep)) {
                    stack.add(dep);
                }
            }
        }
        return visited;
    }

    public Set<ModuleId> dependentsOf(Set<ModuleId> roots) {
        Set<ModuleId> visited = new LinkedHashSet<>(roots);
        Deque<ModuleId> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            ModuleId current = stack.removeLast();
            for (ModuleId dependent : dependents.getOrDefault(current, Set.of())) {
                if (visited.add(dependent)) {
                    stack.add(dependent);
                }
            }
        }
        return visited;
    }

    public List<ModuleId> topologicalSort() {
        Map<ModuleId, Integer> state = new HashMap<>();
        List<ModuleId> order = new ArrayList<>();
        Deque<ModuleId> stack = new ArrayDeque<>();

        for (ModuleId id : metadataById.keySet()) {
            if (state.getOrDefault(id, 0) == 0) {
                dfs(id, state, order, stack);
            }
        }

        return order;
    }

    private void dfs(ModuleId id, Map<ModuleId, Integer> state, List<ModuleId> order, Deque<ModuleId> stack) {
        int st = state.getOrDefault(id, 0);
        if (st == 2) {
            return;
        }
        if (st == 1) {
            throw new ModuleCycleException(buildCycle(stack, id));
        }

        state.put(id, 1);
        stack.addLast(id);

        for (ModuleId dep : dependencies.getOrDefault(id, Set.of())) {
            if (!metadataById.containsKey(dep)) {
                continue;
            }
            dfs(dep, state, order, stack);
        }

        stack.removeLast();
        state.put(id, 2);
        order.add(id);
    }

    private static List<ModuleId> buildCycle(Deque<ModuleId> stack, ModuleId repeated) {
        List<ModuleId> cycle = new ArrayList<>();
        boolean collecting = false;
        for (ModuleId id : stack) {
            if (id.equals(repeated)) {
                collecting = true;
            }
            if (collecting) {
                cycle.add(id);
            }
        }
        cycle.add(repeated);
        return cycle;
    }

    public static final class ModuleCycleException extends IllegalStateException {
        private final List<ModuleId> cycle;

        ModuleCycleException(List<ModuleId> cycle) {
            super("Cyclic module dependencies: " + join(cycle));
            this.cycle = List.copyOf(cycle);
        }

        public List<ModuleId> cycle() {
            return cycle;
        }

        private static String join(List<ModuleId> cycle) {
            List<String> parts = new ArrayList<>();
            for (ModuleId id : cycle) {
                parts.add(id.toString());
            }
            return String.join(" -> ", parts);
        }
    }
}
