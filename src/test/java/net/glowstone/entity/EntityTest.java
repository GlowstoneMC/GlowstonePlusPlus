package net.glowstone.entity;

import static org.mockito.Answers.RETURNS_SMART_NULLS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.logging.Logger;
import net.glowstone.GlowServer;
import net.glowstone.GlowWorld;
import net.glowstone.chunk.GlowChunk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Superclass for tests of entity classes. Doesn't contain tests of its own; instead, configures
 * necessary mocks for subclasses.
 */
@PrepareForTest({GlowWorld.class, GlowServer.class})
@RunWith(PowerMockRunner.class)
public abstract class EntityTest {

    // Mocks
    protected final Logger log = Logger.getLogger(getClass().getSimpleName());
    protected final GlowWorld world = PowerMockito.mock(GlowWorld.class, Mockito.RETURNS_SMART_NULLS);
    protected final GlowServer server = PowerMockito.mock(GlowServer.class, Mockito.RETURNS_SMART_NULLS);
    @Mock
    protected ItemFactory itemFactory;
    @Mock(answer = RETURNS_SMART_NULLS)
    protected GlowChunk chunk;

    // Real objects
    protected Location origin;
    protected final EntityIdManager idManager = new EntityIdManager();
    protected final EntityManager entityManager = new EntityManager();

    @Before
    public void setUp() throws IOException {
        when(server.getLogger()).thenReturn(log);
        if (Bukkit.getServer() == null) {
            Bukkit.setServer(server);
        }
        MockitoAnnotations.initMocks(this);
        origin = new Location(world, 0, 0, 0);
        when(world.getServer()).thenReturn(server);
        when(world.getEntityManager()).thenReturn(entityManager);
        when(world.getChunkAt(any(Location.class))).thenReturn(chunk);
        when(world.getChunkAt(any(Block.class))).thenReturn(chunk);
        when(world.getChunkAt(anyInt(),anyInt())).thenReturn(chunk);
        when(server.getWorlds()).thenReturn(Collections.singletonList(world));
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(server.getEntityIdManager()).thenReturn(idManager);

        // ensureServerConversions returns its argument
        when(itemFactory.ensureServerConversions(any(ItemStack.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }
}
