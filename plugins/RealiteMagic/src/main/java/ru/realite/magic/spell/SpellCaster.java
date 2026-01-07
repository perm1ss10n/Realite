package ru.realite.magic.spell;

import java.util.Objects;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;

public final class SpellCaster {

    private final MagicService magicService;
    private final MagicMessages messages;

    public SpellCaster(MagicService magicService, MagicMessages messages) {
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void cast(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        double currentMana = magicService.getMana(player);
        if (currentMana < spell.mana()) {
            player.sendMessage(messages.msg("magic.error.not_enough_mana",
                    "current", formatNumber(currentMana, "casting.manaFormat", "0.0"),
                    "need", formatNumber(spell.mana(), "casting.manaFormat", "0.0")));
            return;
        }
        long remainingGlobalTicks = magicService.remainingGlobalCooldownTicks(player);
        long remainingSpellTicks = magicService.remainingCooldownTicks(player, spell.id());
        long remainingTicks = Math.max(remainingGlobalTicks, remainingSpellTicks);
        if (remainingTicks > 0) {
            double seconds = remainingTicks / 20.0;
            player.sendMessage(messages.msg("magic.error.on_cooldown",
                    "seconds", formatNumber(seconds, "casting.cooldownFormat", "0.0")));
            return;
        }
        magicService.consumeManaAndCooldowns(player, spell);
        switch (spell.type()) {
            case RAY_DAMAGE -> castRayDamage(player, spell);
        }
    }

    private void castRayDamage(Player player, SpellDefinition spell) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        RayTraceResult result = world.rayTrace(
                eye,
                direction,
                spell.range(),
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                0.2,
                entity -> entity != player);

        Location end = (result != null && result.getHitPosition() != null)
                ? result.getHitPosition().toLocation(world)
                : eye.clone().add(direction.multiply(spell.range()));

        spawnRayParticles(world, eye, end);
        world.playSound(eye, Sound.ENTITY_ENDER_EYE_DEATH, 0.6f, 1.6f);

        if (result != null) {
            Entity hitEntity = result.getHitEntity();
            if (hitEntity instanceof LivingEntity living) {
                living.damage(spell.damage(), player);
                String displayName = messages.raw(spell.nameKey());
                player.sendMessage(messages.msg("magic.spell.cast",
                        "name", displayName));
            }
        }
    }

    private void spawnRayParticles(World world, Location start, Location end) {
        Vector diff = end.toVector().subtract(start.toVector());
        double length = diff.length();
        if (length <= 0) {
            return;
        }
        Vector step = diff.normalize().multiply(0.3);
        int steps = (int) (length / 0.3);
        Location point = start.clone();
        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
            point.add(step);
        }
    }

    private String formatNumber(double value, String configKey, String fallbackPattern) {
        String pattern = magicService.configString(configKey, fallbackPattern);
        DecimalFormat format;
        try {
            format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ex) {
            format = new DecimalFormat(fallbackPattern, DecimalFormatSymbols.getInstance(Locale.US));
        }
        return format.format(value);
    }
}
