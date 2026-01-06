package ru.realite.core.impl;

import org.junit.jupiter.api.Test;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.EventBus;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Scheduler;
import ru.realite.core.api.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleManagerImplTest {

    @Test
    void enableRespectsDependenciesAndDisableIsReversed() throws Exception {
        List<String> enableOrder = new ArrayList<>();
        List<String> disableOrder = new ArrayList<>();

        Module moduleC = new TestModule("module-c", Set.of(), enableOrder, disableOrder);
        Module moduleB = new TestModule("module-b", Set.of(new ModuleId("module-c")), enableOrder, disableOrder);
        Module moduleA = new TestModule("module-a", Set.of(new ModuleId("module-b")), enableOrder, disableOrder);

        ModuleManagerImpl manager = new ModuleManagerImpl(testCore());
        manager.register(moduleA);
        manager.register(moduleB);
        manager.register(moduleC);

        manager.enableAll();
        assertEquals(List.of("module-c", "module-b", "module-a"), enableOrder);

        manager.disableAll();
        assertEquals(List.of("module-a", "module-b", "module-c"), disableOrder);
    }

    @Test
    void cycleIsDetected() throws Exception {
        Module moduleA = new TestModule("cycle-a", Set.of(new ModuleId("cycle-b")));
        Module moduleB = new TestModule("cycle-b", Set.of(new ModuleId("cycle-a")));

        ModuleManagerImpl manager = new ModuleManagerImpl(testCore());
        manager.register(moduleA);
        manager.register(moduleB);

        assertThrows(ModuleGraph.ModuleCycleException.class, manager::loadAll);
    }

    private static CoreApi testCore() throws IOException {
        Platform platform = new TestPlatform();
        Scheduler scheduler = new TestScheduler();
        Services services = new ServicesImpl(scheduler);
        EventBus eventBus = new SimpleEventBus(platform);
        Path dataDir = Files.createTempDirectory("realite-core-test");

        return new CoreApi() {
            @Override
            public Platform platform() {
                return platform;
            }

            @Override
            public Services services() {
                return services;
            }

            @Override
            public EventBus events() {
                return eventBus;
            }

            @Override
            public Path dataDirectory() {
                return dataDir;
            }
        };
    }

    private static final class TestModule implements Module {
        private final ModuleMetadata metadata;
        private final List<String> enableOrder;
        private final List<String> disableOrder;

        private TestModule(String id, Set<ModuleId> dependencies) {
            this(id, dependencies, new ArrayList<>(), new ArrayList<>());
        }

        private TestModule(String id, Set<ModuleId> dependencies, List<String> enableOrder, List<String> disableOrder) {
            this.metadata = new ModuleMetadata(new ModuleId(id), dependencies);
            this.enableOrder = enableOrder;
            this.disableOrder = disableOrder;
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
            enableOrder.add(metadata.id().value());
        }

        @Override
        public void onDisable(ModuleContext ctx) {
            disableOrder.add(metadata.id().value());
        }
    }

    private static final class TestPlatform implements Platform {
        @Override
        public void info(String message) {
            // no-op
        }

        @Override
        public void warn(String message) {
            // no-op
        }

        @Override
        public void debug(String message) {
            // no-op
        }

        @Override
        public void error(String message, Throwable t) {
            // no-op
        }
    }

    private static final class TestScheduler implements Scheduler {
        @Override
        public void runSync(Runnable task) {
            // no-op
        }

        @Override
        public void runLater(Runnable task, long delayTicks) {
            // no-op
        }

        @Override
        public void runRepeating(Runnable task, long delayTicks, long periodTicks) {
            // no-op
        }
    }
}
