package ru.realite.guilds.model;

import java.util.UUID;

public record Guild(String tag, String name, UUID owner) {
}
