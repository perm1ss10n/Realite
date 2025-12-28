package ru.realite.core.impl;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.ConfigService;
import ru.realite.core.api.EventBus;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Scheduler;
import ru.realite.core.api.Services;
import ru.realite.core.api.StorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class ModuleContextImpl implements ModuleContext {

    private final CoreApi core;
    private final ModuleMetadata metadata;
    private final Path dataFolder;

    ModuleContextImpl(CoreApi core, ModuleMetadata metadata, Path dataFolder) {
        this.core = Objects.requireNonNull(core, "core");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create module data folder: " + dataFolder, e);
        }
    }

    @Override
    public CoreApi core() {
        return core;
    }

    @Override
    public Services services() {
        return core.services();
    }

    @Override
    public EventBus events() {
        return core.events();
    }

    @Override
    public Scheduler scheduler() {
        return core.services().scheduler();
    }

    @Override
    public Platform logger() {
        return core.platform();
    }

    @Override
    public Path dataFolder() {
        return dataFolder;
    }

    @Override
    public ConfigService configs() {
        return core.services().require(ConfigService.class);
    }

    @Override
    public StorageService storage() {
        return core.services().require(StorageService.class);
    }

    ModuleMetadata metadata() {
        return metadata;
    }
}
