package net.glowstone.command.minecraft;

import net.glowstone.util.lang.I;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.VanillaCommand;

import java.util.Collections;
import java.util.List;

public class SaveToggleCommand extends VanillaCommand {
    private final boolean on;

    public SaveToggleCommand(boolean on) {
        super(on ? "save-on" : "save-off", on ? I.tr("command.minecraft.save-on.description") : I.tr("command.minecraft.save-off.description"), on ? "/save-on" : "/save-off", Collections.emptyList());
        this.on = on;
        setPermission(on ? "minecraft.command.save-on" : "minecraft.command.save-off");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        for (World world : sender.getServer().getWorlds()) {
            world.setAutoSave(on);
        }
        if (on) {
            sender.sendMessage(I.tr(sender, "command.minecraft.save-on.toggle"));
        } else {
            sender.sendMessage(I.tr(sender, "command.minecraft.save-off.toggle"));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return Collections.emptyList();
    }
}
