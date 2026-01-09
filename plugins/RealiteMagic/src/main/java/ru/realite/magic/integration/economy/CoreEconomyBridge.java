package ru.realite.magic.integration.economy;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;

public final class CoreEconomyBridge implements EconomyBridge {

    private static final String SERVICE_CLASS = "ru.realite.core.api.economy.EconomyService";

    private final Supplier<CoreApi> coreApiSupplier;
    private final Logger logger;
    private boolean warned;

    public CoreEconomyBridge(Logger logger) {
        this(logger, CoreEconomyBridge::resolveCoreApi);
    }

    public CoreEconomyBridge(Logger logger, Supplier<CoreApi> coreApiSupplier) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.coreApiSupplier = Objects.requireNonNull(coreApiSupplier, "coreApiSupplier");
    }

    @Override
    public boolean isAvailable() {
        return economyService() != null;
    }

    @Override
    public double balance(UUID playerId) {
        Object service = economyService();
        if (service == null || playerId == null) {
            return 0.0;
        }
        Object result = invoke(service, "balance", new Class<?>[]{UUID.class}, new Object[]{playerId});
        if (result instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        Object service = economyService();
        if (service == null || playerId == null) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        Object result = invoke(service, "withdraw", new Class<?>[]{UUID.class, double.class},
                new Object[]{playerId, amount});
        if (result instanceof Boolean bool) {
            return bool;
        }
        return false;
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        Object service = economyService();
        if (service == null || playerId == null || amount <= 0) {
            return;
        }
        invoke(service, "deposit", new Class<?>[]{UUID.class, double.class}, new Object[]{playerId, amount});
    }

    @Override
    public Component currencyName() {
        Object service = economyService();
        if (service == null) {
            return Component.empty();
        }
        Object result = invoke(service, "currencyName", new Class<?>[]{}, new Object[]{});
        if (result instanceof Component component) {
            return component;
        }
        if (result instanceof String text) {
            return Component.text(text);
        }
        return Component.empty();
    }

    private Object economyService() {
        CoreApi core = coreApiSupplier.get();
        if (core == null) {
            return null;
        }
        Class<?> serviceClass = resolveServiceClass();
        if (serviceClass == null) {
            return null;
        }
        Object service = core.services().get(serviceClass);
        if (service == null) {
            warnMissingService(null);
        }
        return service;
    }

    private Object invoke(Object target, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, types);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            warnMissingService(ex);
            return null;
        }
    }

    private void warnMissingService(Throwable throwable) {
        if (warned) {
            return;
        }
        warned = true;
        String message = "[Magic] Core economy service is unavailable.";
        if (throwable == null) {
            logger.warning(message);
            return;
        }
        logger.log(Level.WARNING, message, throwable);
    }

    private static CoreApi resolveCoreApi() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider();
    }

    private static Class<?> resolveServiceClass() {
        try {
            return Class.forName(SERVICE_CLASS);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
}
