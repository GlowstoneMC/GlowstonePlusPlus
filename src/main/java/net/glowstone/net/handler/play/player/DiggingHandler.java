package net.glowstone.net.handler.play.player;

import static net.glowstone.net.message.play.player.DiggingMessage.START_DIGGING;

import com.flowpowered.network.MessageHandler;
import java.util.Collection;
import java.util.Objects;
import net.glowstone.EventFactory;
import net.glowstone.GlowWorld;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.ItemTable;
import net.glowstone.block.blocktype.BlockContainer;
import net.glowstone.block.blocktype.BlockType;
import net.glowstone.block.itemtype.ItemTimedUsage;
import net.glowstone.block.itemtype.ItemType;
import net.glowstone.constants.GameRules;
import net.glowstone.datapack.tags.ExtraMaterialTags;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.entity.objects.GlowItem;
import net.glowstone.net.GlowSession;
import net.glowstone.net.message.play.player.DiggingMessage;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

public final class DiggingHandler implements MessageHandler<GlowSession, DiggingMessage> {

    @Override
    public void handle(GlowSession session, DiggingMessage message) {
        GlowPlayer player = session.getPlayer();
        GlowWorld world = player.getWorld();
        EventFactory eventFactory = EventFactory.getInstance();
        GlowBlock block = world.getBlockAt(message.getX(), message.getY(), message.getZ());
        BlockFace face = BlockPlacementHandler.convertFace(message.getFace());
        ItemStack holding = player.getItemInHand();

        if (block.getRelative(face).getType() == Material.FIRE) {
            block.getRelative(face).breakNaturally();
            return; // returns to avoid breaking block in creative
        }

        boolean blockBroken = false;
        boolean revert = false;
        Material material = block.getType();
        switch (message.getState()) {
            case START_DIGGING:
                if (block.equals(player.getDigging()) || block.isLiquid()) {
                    return;
                }
                // call interact event
                Action action = Action.LEFT_CLICK_BLOCK;
                Block eventBlock = block;
                if (player.getLocation().distanceSquared(block.getLocation()) > 36
                        || ExtraMaterialTags.AIR_VARIANTS.isTagged(material)) {
                    action = Action.LEFT_CLICK_AIR;
                    eventBlock = null;
                }
                PlayerInteractEvent interactEvent = eventFactory
                    .onPlayerInteract(player, action, EquipmentSlot.HAND, eventBlock, face);

                // blocks don't get interacted with on left click, so ignore that
                // attempt to use item in hand, that is, dig up the block
                if (!BlockPlacementHandler.selectResult(interactEvent.useItemInHand(), true)) {
                    // the event was cancelled, get out of here
                    revert = true;
                } else if (player.getGameMode() != GameMode.SPECTATOR) {
                    player.setDigging(null);
                    // emit damage event - cancel by default if holding a sword
                    boolean instaBreak = player.getGameMode() == GameMode.CREATIVE
                        || block.getMaterialValues().getHardness() == 0;
                    BlockDamageEvent damageEvent = new BlockDamageEvent(player, block,
                        player.getItemInHand(), instaBreak);
                    if (player.getGameMode() == GameMode.CREATIVE && holding != null
                        && EnchantmentTarget.WEAPON.includes(holding.getType())) {
                        damageEvent.setCancelled(true);
                    }
                    eventFactory.callEvent(damageEvent);

                    // follow orders
                    if (damageEvent.isCancelled()) {
                        revert = true;
                    } else {
                        // in creative, break even if denied in the event, or the block
                        // can never be broken (client does not send DONE_DIGGING).
                        blockBroken = damageEvent.getInstaBreak();
                        if (!blockBroken) {
                            // TODO: add a delay here based on hardness
                            player.setDigging(block);
                        }
                    }
                }
                break;
            case DiggingMessage.CANCEL_DIGGING:
                player.setDigging(null);
                break;
            case DiggingMessage.FINISH_DIGGING:
                // Update client with block
                // (FINISH_DIGGING is client's guess based on wall-clock time, not ticks, and is
                // untrusted)
                break;
            case DiggingMessage.STATE_DROP_ITEM:
                player.dropItemInHand(false);
                return;
            case DiggingMessage.STATE_DROP_ITEMSTACK:
                player.dropItemInHand(true);
                return;
            case DiggingMessage.STATE_SHOT_ARROW_FINISH_EATING:
                final ItemStack usageItem = player.getUsageItem();
                if (usageItem != null) {
                    if (Objects.equals(usageItem, holding)) {
                        ItemType type = ItemTable.instance().getItem(usageItem.getType());
                        if (type != null && type instanceof ItemTimedUsage) {
                            ((ItemTimedUsage) type).endUse(player, usageItem);
                        } else {
                            // todo: inform the player that this item cannot be consumed/used
                        }
                    } else {
                        // todo: verification against malicious clients
                        // todo: inform player their item is wrong
                    }
                }
                return;
            case DiggingMessage.SWAP_ITEM_IN_HAND:
                ItemStack main = player.getInventory().getItemInMainHand();
                ItemStack off = player.getInventory().getItemInOffHand();
                PlayerSwapHandItemsEvent event = EventFactory.getInstance().callEvent(
                    new PlayerSwapHandItemsEvent(player, off, main));
                if (!event.isCancelled()) {
                    player.getInventory().setItemInOffHand(main);
                    player.getInventory().setItemInMainHand(off);
                    player.updateInventory();
                }
                return;
            default:
                return;
        }

        if (blockBroken && !revert) {
            // fire the block break event
            BlockBreakEvent breakEvent = eventFactory.callEvent(new BlockBreakEvent(block, player));
            if (breakEvent.isCancelled()) {
                BlockPlacementHandler.revert(player, block);
                return;
            }

            MaterialData data = block.getState().getData();
            if (ExtraMaterialTags.BISECTED_BLOCKS.isTagged(material)) {
                if (block.getRelative(BlockFace.DOWN).getType() == material) {
                    block = block.getRelative(BlockFace.DOWN);
                }
            }

            BlockType blockType = ItemTable.instance().getBlock(material);
            if (blockType != null) {
                blockType.blockDestroy(player, block, face);
            }

            // destroy the block
            if (!block.isEmpty() && !block.isLiquid() && (player.getGameMode() != GameMode.CREATIVE
                || blockType instanceof BlockContainer) && world.getGameRuleMap()
                .getBoolean(GameRules.DO_TILE_DROPS)) {
                Collection<ItemStack> drops = blockType.getDrops(block, holding);
                if (blockType instanceof BlockContainer
                    && player.getGameMode() == GameMode.CREATIVE) {
                    drops = ((BlockContainer) blockType).getContentDrops(block);
                }
                for (ItemStack drop : drops) {
                    GlowItem item = world.dropItemNaturally(block.getLocation(), drop);
                    item.setPickupDelay(30);
                    item.setBias(player);
                }
            }

            player.addExhaustion(0.005f);

            // STEP_SOUND actually is the block break particles
            world.playEffectExceptTo(block.getLocation(), Effect.STEP_SOUND,
                block.getType().getId(), 64, player);
            GlowBlockState state = block.getState();
            block.setType(Material.AIR);
            if (blockType != null) {
                blockType.afterDestroy(player, block, face, state);
            }
        } else if (revert) {
            // replace the block that wasn't really dug
            BlockPlacementHandler.revert(player, block);
        } else if (!ExtraMaterialTags.AIR_VARIANTS.isTagged(material)) {
            BlockType blockType = ItemTable.instance().getBlock(material);
            blockType.leftClickBlock(player, block, holding);
        }
    }
}
