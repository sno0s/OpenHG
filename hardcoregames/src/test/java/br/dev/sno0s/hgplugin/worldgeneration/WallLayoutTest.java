package br.dev.sno0s.hgplugin.worldgeneration;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WallLayoutTest {
    @Test
    void perimeterIsClosedAndTowersHaveBoundedHeight() {
        TerrainProfile terrain = TerrainProfile.classic();
        WallLayout layout = new WallLayout(terrain, 42, 500, 10);
        Map<String, WallLayout.Column> columns = new HashMap<>();
        for (WallLayout.Column column : layout.columns()) {
            assertNull(columns.put(column.x() + "," + column.z(), column), "Duplicate wall column");
            assertTrue(column.top() >= layout.wallTop());
            assertTrue(column.top() <= layout.wallTop() + 4, "Tower accumulated height from another wall");
            assertTrue(column.top() - terrain.heightAt(42, column.x(), column.z()) >= 10);
        }
        for (int p = -250; p <= 250; p++) {
            for (String key : new String[]{p + ",-250", p + ",250", "-250," + p, "250," + p}) {
                WallLayout.Column column = columns.get(key);
                assertNotNull(column, "Gap in perimeter: " + key);
                assertTrue(column.deepFoundation(), "Tunnel below wall: " + key);
                assertFalse(column.roofOnly());
            }
        }
        assertEquals(layout.wallTop() + 4, columns.get("-250,-250").top());
        assertEquals(layout.wallTop() + 4, columns.get("250,250").top());
        assertEquals(layout.wallTop() + 2, columns.get("0,-250").top());
        System.out.println("Wall plan: " + columns.size() + " columns, top Y=" + layout.wallTop());
    }
}
