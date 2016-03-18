/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;

/**
 *
 * @author antony
 */
public class DuckLogCommand extends AbstractCommand {

    public DuckLogCommand(Main main) {
        super(main);
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) throws CommandException {
        if (args.length != 1 || !"reload".equals(args[0])) {
            throw new UsageException();
        }
        sender.sendMessage(main.getMessage("ducklog.reload"));
        main.reload();
    }

    @Override
    public List<String> tabComplete(String label, String[] args) {
        // TODO
        return null;
    }
    
}
