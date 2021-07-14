package net.glowstone.command.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.state.BlockStateData;
import net.glowstone.block.state.InvalidBlockStateException;
import net.glowstone.block.state.StateSerialization;
import net.glowstone.command.CommandUtils;
import net.glowstone.constants.ItemIds;
import net.glowstone.i18n.LocalizedStringImpl;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

public class FillCommand extends GlowVanillaCommand {

    /**
     * Creates the instance for this command.
     */
    public FillCommand() {
        super("fill");
        setPermission("minecraft.command.fill"); // NON-NLS
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args,
            CommandMessages commandMessages) {
        if (!testPermission(sender, commandMessages.getPermissionMessage())) {
            return true;
        }
        if (args.length < 7) {
            sendUsageMessage(sender, commandMessages);
            return false;
        }
        String itemName = CommandUtils.toNamespaced(args[6].toLowerCase());
        Material type = ItemIds.getBlock(itemName);
        if (type == null) {
            new LocalizedStringImpl("setblock.invalid.type",
                    commandMessages.getResourceBundle())
                    .sendInColor(ChatColor.RED, sender, itemName);
            return false;
        }
        byte dataValue = 0;
        if (args.length > 7) {
            String state = args[7];
            BlockStateData data = CommandUtils.readState(sender, type, state);
            if (data == null) {
                return false;
            }
            if (data.isNumeric()) {
                dataValue = data.getNumericValue();
            } else {
                try {
                    dataValue = StateSerialization.parseData(type, data).getData();
                } catch (InvalidBlockStateException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                    return false;
                }
            }
        }
        Location location1 = CommandUtils.getLocation(sender, args[0], args[1], args[2]);
        Location location2 = CommandUtils.getLocation(sender, args[3], args[4], args[5]);
        double x1 = location1.getX();
        double y1 = location1.getY();
        double z1 = location1.getZ();
        double x2 = location2.getX();
        double y2 = location2.getY();
        double z2 = location2.getZ();
        double xmin = 0;
        double ymin = 0;
        double zmin = 0;
        double xmax = 0;
        double ymax = 0;
        double zmax = 0;
        double placeX;
        double placeY;
        double placeZ;
        if (x1 > x2) {
            xmin = x2;
            xmax = x1;
        } else {
            xmax = x2;
            xmin = x1;
        }
        if (y1 > y2) {
            ymin = y2;
            ymax = y1;
        } else {
            ymax = y2;
            ymin = y1;
        }
        if (z1 > z2) {
            zmin = z2;
            zmax = z1;
        } else {
            zmax = z2;
            zmin = z1;
        }
        double blockAmount = (xmax - xmin) * (ymax - ymin) * (zmax - zmin);
        if (blockAmount > 40000) {
            new LocalizedStringImpl("fill.tooManyBlocks",
            commandMessages.getResourceBundle())
                .sendInColor(ChatColor.RED, sender, Double.toString(blockAmount));
            return false;
        }
        int placementMode = 0;
        if (args.length > 8) {
            if (args[8].equalsIgnoreCase("outline")) {
                placementMode = 1;
            }
            if (args[8].equalsIgnoreCase("hollow")) {
                placementMode = 2;
            }
            if (args[8].equalsIgnoreCase("keep")) {
                placementMode = 3;
            }
            if (args[8].equalsIgnoreCase("destroy")) {
                placementMode = 4;
            }
            if (args[8].equalsIgnoreCase("replace") && args.length >= 10) {
                placementMode = 5;
            }
        }
        if (placementMode == 0 || placementMode == 3 || placementMode == 4 || placementMode == 5) {
            for (placeX = xmin; placeX <= xmax; placeX++) {
                for (placeY = ymin; placeY <= ymax; placeY++) {
                    for (placeZ = zmin; placeZ <= zmax; placeZ++) {
                        Location location = CommandUtils
                            .getLocation(CommandUtils.getLocation(sender), Double.toString(placeX), Double.toString(placeY), Double.toString(placeZ));
                        if (!placeBlock(location, dataValue, args, type, commandMessages, sender, placementMode)) {
                            return false;
                        }
                    }
                }
            }
        }
        if (placementMode == 1 || placementMode == 2) {
            List<Location> blocks = new ArrayList<>();
            for (placeX = xmin; placeX <= xmax; placeX++) {
                for (placeY = ymin; placeY <= ymax; placeY++) {
                    for (placeZ = zmin; placeZ <= zmax; placeZ++) {
                        Location location = CommandUtils
                            .getLocation(CommandUtils.getLocation(sender), Double.toString(placeX), Double.toString(placeY), Double.toString(placeZ));
                        blocks.add(location);
                    }
                }
            }
            xmin += 1;
            xmax -= 1;
            ymin += 1;
            ymax -= 1;
            zmin += 1;
            zmax -= 1;
            for (placeX = xmin; placeX <= xmax; placeX++) {
                for (placeY = ymin; placeY <= ymax; placeY++) {
                    for (placeZ = zmin; placeZ <= zmax; placeZ++) {
                        Location location = CommandUtils
                            .getLocation(CommandUtils.getLocation(sender), Double.toString(placeX), Double.toString(placeY), Double.toString(placeZ));
                        blocks.remove(location);
                        if (placementMode == 2) {
                            if (!placeBlock(location, (byte) 0, args, Material.AIR, commandMessages, sender, placementMode)) {
                                return false;
                            }
                        }
                    }
                }
            }
            for (Location location : blocks) {
                if (!placeBlock(location, dataValue, args, type, commandMessages, sender, placementMode)) {
                    return false;
                }
            }
        }
        new LocalizedStringImpl("fill.done", commandMessages.getResourceBundle())
                .send(sender);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        if (args.length == 4) {
            return ItemIds.getTabCompletion(args[3]);
        }
        return Collections.emptyList();
    }

    public boolean placeBlock(Location location, byte dataValue, String[] args, Material type, CommandMessages commandMessages, CommandSender sender, int placementMode) {
        if (placementMode == 0 || placementMode == 1 || placementMode == 2) {
            GlowBlock block = (GlowBlock) location.getBlock();
            block.setType(type, dataValue, true);
        }
        if (placementMode == 3) {
            GlowBlock block = (GlowBlock) location.getBlock();
            if (block.getTypeId() == 0) {
                block.setType(type, dataValue, true);
            }
        }
        if (placementMode == 4) {
            GlowBlock block = (GlowBlock) location.getBlock();
            if (block.getTypeId() != 0) {
                block.breakNaturally(1.0f);
            }
            block.setType(type, dataValue, true);
        }
        if (placementMode == 5) {
            GlowBlock block = (GlowBlock) location.getBlock();
            String itemNameReplace = CommandUtils.toNamespaced(args[9].toLowerCase());
            Material replaceMaterial = ItemIds.getBlock(itemNameReplace);
            if (replaceMaterial == null) {
                new LocalizedStringImpl("setblock.invalid.type",
                commandMessages.getResourceBundle())
                    .sendInColor(ChatColor.RED, sender, args[9]);
                return false;
            }
            if (block.getType() == replaceMaterial) {
                if (args.length >= 11) {
                    String state = args[10];
                    BlockStateData data = CommandUtils.readState(sender, type, state);
                    Byte dataValueReplace;
                    if (data == null) {
                        return false;
                    }
                    if (data.isNumeric()) {
                        dataValueReplace = data.getNumericValue();
                    } else {
                        try {
                            dataValueReplace = StateSerialization.parseData(type, data).getData();
                        } catch (InvalidBlockStateException e) {
                            sender.sendMessage(ChatColor.RED + e.getMessage());
                            return false;
                        }
                    }
                    if (block.getData() == dataValueReplace) {
                        block.setType(type, dataValue, true);
                    }
                }
                if (args.length < 11) {
                    block.setType(type, dataValue, true);
                }
            }
        }

        return true;
    }
}
