package net.glowstone.generator.objects.trees;

import java.util.Random;
import net.glowstone.util.BlockStateDelegate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;

public class JungleBush extends GenericTree {

    /**
     * Initializes this bush, preparing it to attempt to generate.
     *  @param random the PRNG
     * @param delegate the BlockStateDelegate used to check for space and to fill wood and leaf
     */
    public JungleBush(Random random, BlockStateDelegate delegate) {
        super(random, delegate);
        setTypes(3, 0);
    }

    @Override
    public boolean canPlaceOn(Location loc) {
        BlockState state = delegate
            .getBlockState(loc.getBlock().getRelative(BlockFace.DOWN).getLocation());
        return state.getType() == Material.GRASS || state.getType() == Material.DIRT;
    }

    @Override
    public boolean generate(Location loc) {
        Location l = loc.clone();
        while ((l.getBlock().getType() == Material.AIR || l.getBlock().getType() == Material.LEAVES)
            && l.getBlockY() > 0) {
            l.subtract(0, 1, 0);
        }

        // check only below block
        if (!canPlaceOn(loc)) {
            return false;
        }

        // generates the trunk
        delegate.setTypeAndRawData(l.getWorld(), l.getBlockX(), l.getBlockY() + 1, l.getBlockZ(),
            Material.LOG, logType);

        // generates the leaves
        for (int y = l.getBlockY() + 1; y <= l.getBlockY() + 3; y++) {
            int radius = 3 - (y - l.getBlockY());

            for (int x = l.getBlockX() - radius; x <= l.getBlockX() + radius; x++) {
                for (int z = l.getBlockZ() - radius; z <= l.getBlockZ() + radius; z++) {
                    if ((Math.abs(x - l.getBlockX()) != radius
                            || Math.abs(z - l.getBlockZ()) != radius || random.nextBoolean())
                            && !delegate.getBlockState(l.getWorld(), x, y, z).getType().isSolid()) {
                        delegate
                            .setTypeAndRawData(l.getWorld(), x, y, z, Material.LEAVES, leavesType);
                    }
                }
            }
        }

        return true;
    }
}
