package br.dev.sno0s.hgplugin.worldgeneration;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WallLayoutTest {
    @Test
    void perimeterIsClosedAndTowersHaveBoundedHeight() {
        for (int size : new int[]{256, 500, 750, 1200}) checkPerimeter(size);
    }

    private void checkPerimeter(int size) {
        int half = size / 2;
        TerrainProfile terrain = new TerrainProfile(68, 12, .006, .008, -.15, -.3, size);
        WallLayout layout = new WallLayout(terrain, 42, size, 10);
        Map<String, WallLayout.Column> columns = new HashMap<>();
        for (WallLayout.Column column : layout.columns()) {
            assertNull(columns.put(column.x() + "," + column.z(), column), "Duplicate wall column");
            assertTrue(column.top() >= layout.wallTop());
            assertTrue(column.top() <= layout.wallTop() + 4, "Tower accumulated height from another wall");
            assertTrue(column.top() - terrain.heightAt(42, column.x(), column.z()) >= 10);
        }
        for (int p = -half; p <= half; p++) {
            for (String key : new String[]{p + "," + -half, p + "," + half, -half + "," + p, half + "," + p}) {
                WallLayout.Column column = columns.get(key);
                assertNotNull(column, "Gap in perimeter: " + key);
                assertTrue(column.deepFoundation(), "Tunnel below wall: " + key);
                assertFalse(column.roofOnly());
            }
        }
        assertEquals(layout.wallTop() + 4, columns.get(-half + "," + -half).top());
        assertEquals(layout.wallTop() + 4, columns.get(half + "," + half).top());
        assertEquals(layout.wallTop() + 2, columns.get("0," + -half).top());
        System.out.println("Wall plan: " + columns.size() + " columns, top Y=" + layout.wallTop());
    }
}
