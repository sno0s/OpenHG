package br.dev.sno0s.hgplugin.database;

import java.util.UUID;

public record PlayerStats(
        UUID uuid,
        String name,
        int kills,
        int deaths,
        int wins,
        int matches,
        String lastKit
) {}
