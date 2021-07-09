package net.glowstone.io.nbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.glowstone.GlowOfflinePlayer;
import net.glowstone.GlowServer;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.i18n.ConsoleMessages;
import net.glowstone.i18n.GlowstoneMessages;
import net.glowstone.io.PlayerDataService;
import net.glowstone.io.entity.EntityStorage;
import net.glowstone.util.UuidUtils;
import net.glowstone.util.nbt.CompoundTag;
import net.glowstone.util.nbt.NbtInputStream;
import net.glowstone.util.nbt.NbtOutputStream;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

/**
 * Standard NBT-based player data storage.
 */
public class NbtPlayerDataService implements PlayerDataService {

    private final GlowServer server;
    private final File playerDir;

    public NbtPlayerDataService(GlowServer server, File playerDir) {
        this.server = server;
        this.playerDir = playerDir;
    }

    private File getPlayerFile(UUID uuid) {
        if (!playerDir.isDirectory() && !playerDir.mkdirs()) {
            ConsoleMessages.Warn.Io.MKDIR_FAILED.log(playerDir);
        }
        return new File(playerDir, UuidUtils.toString(uuid) + ".dat");
    }

    private void readDataImpl(GlowPlayer player, CompoundTag playerTag) {
        EntityStorage.load(player, playerTag);
    }

    @Override
    public CompletableFuture<Collection<OfflinePlayer>> getOfflinePlayers() {
        // list files in directory
        File[] files = playerDir.listFiles();
        if (files == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        List<CompletableFuture<GlowOfflinePlayer>> futures = new ArrayList<>(files.length);
        for (File file : files) {
            // first, make sure it looks like a player file
            String name = file.getName();
            if (name.length() != 40 || !name.endsWith(".dat")) { // NON-NLS
                continue;
            }

            // get the UUID
            UUID uuid;
            try {
                uuid = UuidUtils.fromString(name.substring(0, 36));
            } catch (IllegalArgumentException e) {
                continue;
            }

            // creating the OfflinePlayer will read the data
            futures.add(GlowOfflinePlayer.getOfflinePlayer(server, uuid));
        }

        CompletableFuture<Void> gotAll = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        return gotAll.thenApplyAsync(v ->
            futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
    }

    @Override
    public PlayerReader beginReadingData(UUID uuid) {
        return new NbtPlayerReader(getPlayerFile(uuid));
    }

    @Override
    public void readData(GlowPlayer player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        CompoundTag playerTag = new CompoundTag();
        if (playerFile.exists()) {
            try (NbtInputStream in = new NbtInputStream(new FileInputStream(playerFile))) {
                playerTag = in.readCompound();
            } catch (IOException e) {
                player.kickPlayer(GlowstoneMessages.Kick.FILE_READ.get());
                ConsoleMessages.Error.Io.PLAYER_READ.log(e, player.getName(), playerFile);
            }
        }
        readDataImpl(player, playerTag);
    }

    @Override
    public void writeData(GlowPlayer player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        CompoundTag tag = new CompoundTag();
        EntityStorage.save(player, tag);
        try (NbtOutputStream out = new NbtOutputStream(new FileOutputStream(playerFile))) {
            out.writeTag(tag);
        } catch (IOException e) {
            player.kickPlayer(GlowstoneMessages.Kick.FILE_WRITE.get());
            ConsoleMessages.Error.Io.PLAYER_WRITE.log(e, player.getName(), playerFile);
        }
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private class NbtPlayerReader implements PlayerReader {

        private CompoundTag tag = new CompoundTag();
        private boolean hasPlayed;

        public NbtPlayerReader(File playerFile) {
            if (playerFile.exists()) {
                try (NbtInputStream in = new NbtInputStream(new FileInputStream(playerFile))) {
                    tag = in.readCompound();
                    hasPlayed = true;
                } catch (IOException e) {
                    ConsoleMessages.Error.Io.PLAYER_READ_UNKNOWN.log(e, playerFile);
                }
            }
        }

        private void checkOpen() {
            if (tag == null) {
                throw new IllegalStateException("cannot access fields after close");
            }
        }

        @Override
        public boolean hasPlayedBefore() {
            return hasPlayed;
        }

        @Override
        public Location getLocation() {
            checkOpen();
            World world = NbtSerialization.readWorld(server, tag);
            if (world != null) {
                return NbtSerialization.listTagsToLocation(world, tag);
            }
            return null;
        }

        @Override
        public Location getBedSpawnLocation() {
            checkOpen();
            // check that all fields are present
            if (!tag.isString("SpawnWorld")
                || !tag.isInt("SpawnX")
                || !tag.isInt("SpawnY")
                || !tag.isInt("SpawnZ")) {
                return null;
            }
            // look up world
            World world = server.getWorld(tag.getString("SpawnWorld"));
            if (world == null) {
                return null;
            }
            // return location
            return new Location(
                world,
                tag.getInt("SpawnX"),
                tag.getInt("SpawnY"),
                tag.getInt("SpawnZ")
            );
        }

        @Override
        public long getFirstPlayed() {
            checkOpen();
            long[] out = {0};
            tag.readCompound("bukkit", bukkit -> bukkit.readLong("firstPlayed", x -> out[0] = x));
            return out[0];
        }

        @Override
        public long getLastPlayed() {
            checkOpen();
            long[] out = {0};
            // In Paper, the value is stored both in Paper->LastSeen and bukkit->lastPlayed.
            if (tag.containsKey("Paper")) {
                tag.readCompound("Paper", paper -> paper.readLong("LastSeen", x -> out[0] = x));
            } else {
                tag.readCompound("bukkit",
                    bukkit -> bukkit.readLong("lastPlayed", x -> out[0] = x));
            }
            return out[0];
        }

        @Override
        public long getLastLogin() {
            checkOpen();
            long[] out = {0};
            tag.readCompound("Paper", paper -> paper.readLong("LastLogin", x -> out[0] = x));
            return out[0];
        }

        @Override
        public String getLastKnownName() {
            checkOpen();
            String[] out = {null};
            tag.readCompound("bukkit",
                bukkit -> bukkit.readString("lastKnownName", x -> out[0] = x));
            return out[0];
        }

        @Override
        public void readData(GlowPlayer player) {
            checkOpen();
            readDataImpl(player, tag);
        }

        @Override
        public void close() {
            tag = null;
        }
    }
}
