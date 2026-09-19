package br.dev.sno0s.hgplugin.worldgeneration;

import org.junit.jupiter.api.Test;
import java.util.EnumMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TerrainProfileTest {
    @Test
    void hillsStayWalkableAcrossPositiveAndNegativeChunkBoundaries() {
        TerrainProfile terrain = TerrainProfile.classic();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, biggestStep = 0;
        Map<TerrainProfile.Landscape, Integer> biomes = new EnumMap<>(TerrainProfile.Landscape.class);
        for (long seed : new long[]{0, 1, -1, 42, 987654321, Long.MIN_VALUE, Long.MAX_VALUE}) {
            for (int x = -250; x <= 250; x++) {
                for (int z = -250; z <= 250; z++) {
                    int y = terrain.heightAt(seed, x, z);
                    min = Math.min(min, y);
                    max = Math.max(max, y);
                    biggestStep = Math.max(biggestStep, Math.abs(y - terrain.heightAt(seed, x + 1, z)));
                    biggestStep = Math.max(biggestStep, Math.abs(y - terrain.heightAt(seed, x, z + 1)));
                    biomes.merge(terrain.biomeAt(seed, x, z), 1, Integer::sum);
                    assertTrue(y >= 56 && y <= 80, "Classic terrain exceeded its height bounds");
                    assertTrue(Math.abs(y - terrain.heightAt(seed, x + 1, z)) <= 1, "East-west cliff");
                    assertTrue(Math.abs(y - terrain.heightAt(seed, x, z + 1)) <= 1, "North-south cliff");
                }
            }
        }
        assertTrue(max - min >= 10, "Terrain should retain gentle hills, not become a flat world");
        int total = biomes.values().stream().mapToInt(Integer::intValue).sum();
        for (TerrainProfile.Landscape biome : TerrainProfile.Landscape.values()) {
            assertTrue(biomes.getOrDefault(biome, 0) > total * 0.05, "Missing/rare biome: " + biome);
        }
        System.out.println("Classic map samples: Y=" + min + ".." + max + ", max adjacent step=" + biggestStep + ", biomes=" + biomes);
    }

    @Test
    void sameSeedReproducesTerrainAndDifferentSeedsChangeIt() {
        TerrainProfile first = TerrainProfile.classic(), second = TerrainProfile.classic();
        int changed = 0;
        for (int x = -250; x <= 250; x += 7) {
            assertEquals(first.heightAt(42, x, 90), second.heightAt(42, x, 90));
            assertEquals(first.biomeAt(42, x, 90), second.biomeAt(42, x, 90));
            if (first.heightAt(42, x, 90) != first.heightAt(43, x, 90)) changed++;
        }
        assertTrue(changed > 30);
    }

    @Test
    void spawnIsFlatAndVariationCanBeDisabled() {
        TerrainProfile terrain = TerrainProfile.classic();
        TerrainProfile flat = new TerrainProfile(68, 0, 0.006, 0.008, -0.15, -0.3);
        for (int x = -10; x <= 10; x++) {
            assertEquals(68, terrain.heightAt(123, x, 0));
        }
        assertEquals(68, flat.heightAt(123, -200, 190));
    }

    @Test
    void rejectsNonFiniteAndUnsafeSettings() {
        assertThrows(IllegalArgumentException.class, () -> new TerrainProfile(68, Double.NaN, .006, .008, -.15, -.3));
        assertThrows(IllegalArgumentException.class, () -> new TerrainProfile(68, 12, 0, .008, -.15, -.3));
        assertThrows(IllegalArgumentException.class, () -> new TerrainProfile(310, 12, .006, .008, -.15, -.3));
    }
}
