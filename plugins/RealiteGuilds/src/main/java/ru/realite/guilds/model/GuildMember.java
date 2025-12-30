package ru.realite.guilds.model;

import java.time.LocalDate;
import java.util.UUID;

public record GuildMember(UUID uuid, String tag, String role, LocalDate lastSalaryDate) {
}
