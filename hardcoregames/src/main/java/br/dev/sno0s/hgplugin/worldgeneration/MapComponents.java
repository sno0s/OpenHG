package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MapComponents implements Listener {

    // ── Dimensões ────────────────────────────────────────────────
    private static final int WALL_HEIGHT        = 10; // blocos acima do terreno
    private static final int BATTLEMENT_HEIGHT  = 2;  // altura dos merlões
    private static final int CORNER_RADIUS      = 4;  // raio das torres de quina (circulares)
    private static final int CORNER_EXTRA       = 6;  // quanto as torres de quina sobem além da parede
    private static final int MID_TOWER_HALF     = 2;  // meia-largura das torres intermediárias (5×5 total projetado)
    private static final int MID_TOWER_EXTRA    = 3;  // quanto sobem além da parede
    private static final int MID_TOWER_INTERVAL = 60; // blocos entre torres intermediárias

    public static void gerarParede(JavaPlugin plugin, World world, int tamanho) {
        int half = tamanho / 2;
        int minY = world.getMinHeight();

        final int[] x = {-half};
        final int[] z = {-half};

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int placed = 0;

            // 1. Paredes norte/sul — varia x, fixo z = ±half
            while (x[0] <= half && placed < 8) {
                buildNSSlice(world, x[0], -half, minY, true);  // norte (inward = +z)
                buildNSSlice(world, x[0],  half, minY, false); // sul   (inward = -z)
                x[0]++;
                placed++;
            }

            // 2. Paredes oeste/leste — varia z, fixo x = ±half
            if (x[0] > half) {
                while (z[0] <= half && placed < 8) {
                    buildEWSlice(world, -half, z[0], minY, true);  // oeste (inward = +x)
                    buildEWSlice(world,  half, z[0], minY, false); // leste (inward = -x)
                    z[0]++;
                    placed++;
                }
            }

            if (z[0] > half) {
                task.cancel();

                // 3. Torres (pequenas o suficiente para construir de uma vez)
                buildCornerTowers(world, half, minY);
                buildMidTowers(world, half, minY);

                Bukkit.getLogger().info("[HardcoreGames] Muralha finalizada!");
                world.getWorldBorder().setCenter(0, 0);
                world.getWorldBorder().setSize(tamanho);
                Bukkit.getLogger().info("[HardcoreGames] WorldBorder ativada (" + tamanho + " blocos).");
            }
        }, 0L, 1L);
    }

    // ── Paredes ──────────────────────────────────────────────────

    /** Fatia vertical da parede norte/sul (2 blocos de espessura em Z). */
    private static void buildNSSlice(World world, int x, int wallZ, int minY, boolean northward) {
        int zInner = northward ? wallZ + 1 : wallZ - 1;
        int top = wallTop(world, x, wallZ, x, zInner);

        for (int y = minY; y <= top; y++) {
            world.getBlockAt(x, y, wallZ ).setType(wallMat(x, y, wallZ),  false);
            world.getBlockAt(x, y, zInner).setType(wallMat(x, y, zInner), false);
        }
        // ameiões na face externa (2 de largura por ciclo)
        if ((x >> 1 & 1) == 0) {
            for (int h = 1; h <= BATTLEMENT_HEIGHT; h++) {
                world.getBlockAt(x, top + h, wallZ).setType(wallMat(x, top + h, wallZ), false);
            }
        }
    }

    /** Fatia vertical da parede oeste/leste (2 blocos de espessura em X). */
    private static void buildEWSlice(World world, int wallX, int z, int minY, boolean westward) {
        int xInner = westward ? wallX + 1 : wallX - 1;
        int top = wallTop(world, wallX, z, xInner, z);

        for (int y = minY; y <= top; y++) {
            world.getBlockAt(wallX,  y, z).setType(wallMat(wallX,  y, z), false);
            world.getBlockAt(xInner, y, z).setType(wallMat(xInner, y, z), false);
        }
        if ((z >> 1 & 1) == 0) {
            for (int h = 1; h <= BATTLEMENT_HEIGHT; h++) {
                world.getBlockAt(wallX, top + h, z).setType(wallMat(wallX, top + h, z), false);
            }
        }
    }

    // ── Torres de quina ──────────────────────────────────────────

    private static void buildCornerTowers(World world, int half, int minY) {
        int[][] corners = {{-half, -half}, {half, -half}, {-half, half}, {half, half}};

        for (int[] c : corners) {
            int cx = c[0], cz = c[1];
            int top = world.getHighestBlockYAt(cx, cz) + WALL_HEIGHT + CORNER_EXTRA;
            int r = CORNER_RADIUS;

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < r - 0.6 || dist > r + 0.5) continue; // shell circular

                    int bx = cx + dx, bz = cz + dz;
                    for (int y = minY; y <= top; y++) {
                        world.getBlockAt(bx, y, bz).setType(wallMat(bx, y, bz), false);
                    }
                    // ameiões de 2 em 2
                    if (((bx + bz) >> 1 & 1) == 0) {
                        for (int h = 1; h <= BATTLEMENT_HEIGHT; h++) {
                            world.getBlockAt(bx, top + h, bz).setType(wallMat(bx, top + h, bz), false);
                        }
                    }
                }
            }
        }
    }

    // ── Torres intermediárias ─────────────────────────────────────

    private static void buildMidTowers(World world, int half, int minY) {
        for (int pos = -half + MID_TOWER_INTERVAL; pos < half; pos += MID_TOWER_INTERVAL) {
            // Norte (z=-half): protrui para fora (-z) e para dentro (+z)
            buildMidTower(world, pos, -half, minY, false, true);
            // Sul (z=+half): protrui +z e -z
            buildMidTower(world, pos,  half, minY, false, false);
            // Oeste (x=-half): protrui -x e +x
            buildMidTower(world, -half, pos, minY, true, true);
            // Leste (x=+half)
            buildMidTower(world,  half, pos, minY, true, false);
        }
    }

    /**
     * Torre intermediária quadrada (2*MID_TOWER_HALF+1 de largura).
     * Protrui MID_TOWER_HALF blocos para fora e para dentro da parede.
     *
     * @param alongZ  true → parede corre em Z (torres norte/sul), false → corre em X
     * @param outward true → protrui na direção negativa do eixo normal
     */
    private static void buildMidTower(World world, int cx, int cz, int minY,
                                      boolean alongZ, boolean outward) {
        int s = MID_TOWER_HALF;
        int top = world.getHighestBlockYAt(cx, cz) + WALL_HEIGHT + MID_TOWER_EXTRA;
        int protrude = outward ? -s : s;

        for (int da = -s; da <= s; da++) {        // ao longo da parede
            for (int db = -s; db <= s; db++) {    // perpendicular (protrui)
                // apenas o perímetro (oco por dentro)
                if (Math.abs(da) != s && Math.abs(db) != s) continue;

                int bx = alongZ ? cx + da : cx + db * (outward ? -1 : 1);
                int bz = alongZ ? cz + db * (outward ? -1 : 1) : cz + da;

                for (int y = minY; y <= top; y++) {
                    world.getBlockAt(bx, y, bz).setType(wallMat(bx, y, bz), false);
                }
                if (((bx + bz) >> 1 & 1) == 0) {
                    for (int h = 1; h <= BATTLEMENT_HEIGHT; h++) {
                        world.getBlockAt(bx, top + h, bz).setType(wallMat(bx, top + h, bz), false);
                    }
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    /** Altura do topo da parede baseada no terreno mais alto entre dois pontos. */
    private static int wallTop(World world, int x1, int z1, int x2, int z2) {
        return Math.max(
            world.getHighestBlockYAt(x1, z1),
            world.getHighestBlockYAt(x2, z2)
        ) + WALL_HEIGHT;
    }

    /** Material determinístico com variação visual baseada na posição. */
    private static Material wallMat(int x, int y, int z) {
        int h = Math.abs((x * 73856093) ^ (y * 19349663) ^ (z * 83492791)) % 100;
        if (h < 50) return Material.STONE_BRICKS;
        if (h < 68) return Material.CRACKED_STONE_BRICKS;
        if (h < 84) return Material.MOSSY_STONE_BRICKS;
        return Material.MOSSY_COBBLESTONE;
    }
}
