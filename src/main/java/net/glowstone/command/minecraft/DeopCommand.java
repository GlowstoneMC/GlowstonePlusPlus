package net.glowstone.command.minecraft;

import net.glowstone.GlowServer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VanillaCommand;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeopCommand extends VanillaCommand {
    public DeopCommand() {
        super("deop", GlowServer.lang.getString("command.minecraft.deop.description"), "/deop <" + GlowServer.lang.getString("command.minecraft.deop.args.player") + ">", Collections.emptyList());
        setPermission("minecraft.command.deop");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) return false;
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + GlowServer.lang.getString(sender, "command.generic.usage", "/deop <" + GlowServer.lang.getString(sender, "command.minecraft.deop.args.player") + ">"));
            return false;
        }
        String name = args[0];
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        player.setOp(false);
        sender.sendMessage(GlowServer.lang.getString(sender, "command.minecraft.deop.deopped", player.getName()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            List<String> operators = new ArrayList<>();
            Bukkit.getOperators().stream().filter(OfflinePlayer::isOp).filter(player -> player.getName() != null).forEach(player -> operators.add(player.getName()));
            return (List) StringUtil.copyPartialMatches(args[0], operators, new ArrayList(operators.size()));
        } else if (args.length > 1) {
            return Collections.emptyList();
        }
        return super.tabComplete(sender, alias, args);
    }
}
