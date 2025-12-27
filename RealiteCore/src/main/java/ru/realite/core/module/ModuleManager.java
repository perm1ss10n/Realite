package ru.realite.core.module;

import ru.realite.core.CoreContext;
import ru.realite.core.Platform;

import java.util.*;

/**
 * Менеджер модулей:
 * - регистрирует
 * - сортирует по зависимостям
 * - включает/выключает
 */
public final class ModuleManager {

    private final CoreContext ctx;
    private final Platform log;

    private final Map<String, Module> modulesById = new LinkedHashMap<>();
    private final List<Module> enabledOrder = new ArrayList<>();

    public ModuleManager(CoreContext ctx) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.log = ctx.platform();
    }

    public void register(Module module) {
        Objects.requireNonNull(module, "module");
        String id = Objects.requireNonNull(module.id(), "module.id()");
        if (id.isBlank()) throw new IllegalArgumentException("module.id() is blank");

        Module prev = modulesById.putIfAbsent(id, module);
        if (prev != null) {
            throw new IllegalStateException("Module already registered: " + id
                    + " (existing=" + prev.getClass().getName()
                    + ", new=" + module.getClass().getName() + ")");
        }
    }

    public Module get(String id) {
        return modulesById.get(id);
    }

    public Collection<Module> all() {
        return Collections.unmodifiableCollection(modulesById.values());
    }

    public void enableAll() {
        List<Module> ordered = topoSort(modulesById);

        log.info("Enabling modules: " + ordered.stream().map(Module::id).toList());

        for (Module m : ordered) {
            try {
                log.info("Enabling module: " + m.id());
                m.onEnable(ctx);
                enabledOrder.add(m);
            } catch (Exception e) {
                log.error("Failed to enable module: " + m.id(), e);
                // если модуль не включился — безопаснее остановить остальные
                throw new IllegalStateException("Failed to enable module: " + m.id(), e);
            }
        }
    }

    public void disableAll() {
        // выключаем в обратном порядке
        ListIterator<Module> it = enabledOrder.listIterator(enabledOrder.size());
        while (it.hasPrevious()) {
            Module m = it.previous();
            try {
                log.info("Disabling module: " + m.id());
                m.onDisable();
            } catch (Exception e) {
                log.error("Failed to disable module: " + m.id(), e);
            }
        }
        enabledOrder.clear();
    }

    /**
     * Топологическая сортировка по dependsOn().
     * Если зависимость не зарегистрирована — ошибка.
     * Если цикл зависимостей — ошибка.
     */
    private static List<Module> topoSort(Map<String, Module> modules) {
        Map<String, Integer> state = new HashMap<>();
        // 0 - не посещён, 1 - в стеке (visiting), 2 - готов (done)

        List<Module> out = new ArrayList<>();

        for (String id : modules.keySet()) {
            if (state.getOrDefault(id, 0) == 0) {
                dfs(id, modules, state, out, new ArrayDeque<>());
            }
        }

        return out;
    }

    private static void dfs(
            String id,
            Map<String, Module> modules,
            Map<String, Integer> state,
            List<Module> out,
            Deque<String> stack
    ) {
        int st = state.getOrDefault(id, 0);
        if (st == 2) return;
        if (st == 1) {
            // цикл
            stack.addLast(id);
            throw new IllegalStateException("Cyclic module dependencies: " + String.join(" -> ", stack));
        }

        Module m = modules.get(id);
        if (m == null) {
            throw new IllegalStateException("Missing dependency module: " + id);
        }

        state.put(id, 1);
        stack.addLast(id);

        for (String dep : m.dependsOn()) {
            if (!modules.containsKey(dep)) {
                throw new IllegalStateException("Module '" + id + "' depends on missing module '" + dep + "'");
            }
            dfs(dep, modules, state, out, stack);
        }

        stack.removeLast();
        state.put(id, 2);
        out.add(m);
    }
}
