package br.dev.sno0s.hgplugin.worldgeneration;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/** Three curved surface ravines inside the configured arena, away from spawn and walls. */
public final class RavineLayout {
    public record Ravine(double x, double z, double direction, double halfLength,
                         double halfWidth, int depth, double bend) {
        public int depthAt(int blockX, int blockZ) {
            double dx = blockX - x, dz = blockZ - z;
            double along = dx * Math.cos(direction) + dz * Math.sin(direction);
            double progress = along / halfLength;
            if (Math.abs(progress) >= 1) return 0;
            double across = -dx * Math.sin(direction) + dz * Math.cos(direction);
            across -= bend * Math.sin(progress * Math.PI);
            double width = halfWidth * Math.sqrt(1 - progress * progress);
            double side = across / width;
            if (Math.abs(side) >= 1) return 0;
            return (int) Math.round(depth * Math.sqrt((1 - progress * progress) * (1 - side * side)));
        }
    }

    private final List<Ravine> ravines;

    public RavineLayout(long seed, int worldSize) {
        Random random = new Random(seed ^ 0x72D45A2B9C183EF1L);
        double rotation = random.nextDouble() * Math.PI * 2;
        double scale = worldSize / 750.0;
        ravines = IntStream.range(0, 3).mapToObj(i -> {
            double angle = rotation + i * Math.PI * 2 / 3;
            double radius = worldSize * (0.25 + random.nextDouble() * 0.06);
            return new Ravine(Math.cos(angle) * radius, Math.sin(angle) * radius,
                    angle + Math.PI / 2 + (random.nextDouble() - 0.5) * 0.5,
                    (45 + random.nextDouble() * 20) * scale,
                    Math.max(1.5, (3 + random.nextDouble() * 2) * Math.min(scale, 1.5)),
                    24 + random.nextInt(11), (3 + random.nextDouble() * 5) * scale);
        }).toList();
    }

    public List<Ravine> ravines() { return ravines; }

    public int depthAt(int x, int z) {
        int depth = 0;
        for (Ravine ravine : ravines) depth = Math.max(depth, ravine.depthAt(x, z));
        return depth;
    }
}
