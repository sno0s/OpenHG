package br.dev.sno0s.hgplugin.worldgeneration;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ArenaEnvironmentTest {
    @Test
    void restoresDayAndClearWeatherAfterExternalChanges() {
        World arena = mock(World.class);
        when(arena.getTime()).thenReturn(18000L);
        when(arena.hasStorm()).thenReturn(true);
        when(arena.isThundering()).thenReturn(true);
        when(arena.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)).thenReturn(true);
        when(arena.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)).thenReturn(true);
        ArenaEnvironment.apply(arena);
        verify(arena).setTime(1000L);
        verify(arena).setStorm(false);
        verify(arena).setThundering(false);
        verify(arena).setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        verify(arena).setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    @Test
    void stableArenaDoesNotRepeatedlyWriteWorldState() {
        World arena = mock(World.class);
        when(arena.getTime()).thenReturn(1000L);
        when(arena.getGameRuleValue(GameRule.DO_DAYLIGHT_CYCLE)).thenReturn(false);
        when(arena.getGameRuleValue(GameRule.DO_WEATHER_CYCLE)).thenReturn(false);
        ArenaEnvironment.apply(arena);
        verify(arena, never()).setTime(anyLong());
        verify(arena, never()).setStorm(anyBoolean());
        verify(arena, never()).setThundering(anyBoolean());
        verify(arena, never()).setGameRule(any(), any());
    }
}
