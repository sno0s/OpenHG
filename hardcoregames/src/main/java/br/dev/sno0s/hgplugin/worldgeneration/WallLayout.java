package br.dev.sno0s.hgplugin.worldgeneration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plans all heights before placing blocks, so walls never measure their own towers or nearby trees. */
public final class WallLayout {
    public record Column(int x, int z, int top, boolean merlon, boolean deepFoundation,
                         boolean roofOnly, boolean accent) {}

    private final List<Column> columns;
    private final int wallTop;

    public WallLayout(TerrainProfile terrain, long seed, int size, int height) {
        if (size < 64 || size % 2 != 0 || height < 6 || height > 24) {
            throw new IllegalArgumentException("Invalid wall dimensions");
        }
        int half = size / 2;
        int highest = Integer.MIN_VALUE;
        for (int p = -half - 4; p <= half + 4; p++) {
            for (int offset = -4; offset <= 4; offset++) {
                highest = Math.max(highest, terrain.heightAt(seed, p, -half + offset));
                highest = Math.max(highest, terrain.heightAt(seed, p, half + offset));
                highest = Math.max(highest, terrain.heightAt(seed, -half + offset, p));
                highest = Math.max(highest, terrain.heightAt(seed, half + offset, p));
            }
        }
        wallTop = highest + height;
        Map<Long, Column> plan = new LinkedHashMap<>();
        for (int p = -half; p <= half; p++) {
            for (int depth = 0; depth < 3; depth++) {
                boolean merlon = depth == 0 && Math.floorMod(p, 4) < 2;
                put(plan, new Column(p, -half + depth, wallTop, merlon, depth == 0, false, false));
                put(plan, new Column(p, half - depth, wallTop, merlon, depth == 0, false, false));
                put(plan, new Column(-half + depth, p, wallTop, merlon, depth == 0, false, false));
                put(plan, new Column(half - depth, p, wallTop, merlon, depth == 0, false, false));
            }
            if (Math.floorMod(p, 12) == 0) {
                put(plan, new Column(p, -half - 1, wallTop, true, false, false, true));
                put(plan, new Column(p, half + 1, wallTop, true, false, false, true));
                put(plan, new Column(-half - 1, p, wallTop, true, false, false, true));
                put(plan, new Column(half + 1, p, wallTop, true, false, false, true));
            }
        }
        for (int x : new int[]{-half, half}) {
            for (int z : new int[]{-half, half}) tower(plan, x, z, 4, wallTop + 4, true);
        }
        // Symmetric spacing on all four sides, including a tower at each side's midpoint.
        for (int p = 0; p < half - 12; p += 64) {
            for (int sign : p == 0 ? new int[]{1} : new int[]{-1, 1}) {
                int center = p * sign;
                tower(plan, center, -half, 2, wallTop + 2, false);
                tower(plan, center, half, 2, wallTop + 2, false);
                tower(plan, -half, center, 2, wallTop + 2, false);
                tower(plan, half, center, 2, wallTop + 2, false);
            }
        }
        columns = List.copyOf(plan.values());
    }

    private static void tower(Map<Long, Column> plan, int cx, int cz, int radius, int top, boolean round) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distance = Math.hypot(dx, dz);
                if (round && distance > radius + 0.35) continue;
                boolean shell = round ? distance >= radius - 0.8
                        : Math.abs(dx) == radius || Math.abs(dz) == radius;
                boolean merlon = shell && Math.floorMod(dx + dz, 3) != 1;
                put(plan, new Column(cx + dx, cz + dz, top, merlon, false, !shell, shell));
            }
        }
    }

    private static void put(Map<Long, Column> plan, Column column) {
        long key = ((long) column.x() << 32) ^ (column.z() & 0xffffffffL);
        Column previous = plan.get(key);
        if (previous != null) {
            column = new Column(column.x(), column.z(), Math.max(previous.top(), column.top()),
                    column.merlon() || (previous.top() == column.top() && previous.merlon()),
                    previous.deepFoundation() || column.deepFoundation(),
                    previous.roofOnly() && column.roofOnly(), column.accent());
        }
        plan.put(key, column);
    }

    public List<Column> columns() { return columns; }
    public int wallTop() { return wallTop; }
}
