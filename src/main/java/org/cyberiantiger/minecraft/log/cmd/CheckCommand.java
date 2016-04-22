/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cyberiantiger.minecraft.log.Main;

/**
 *
 * @author antony
 */
public class CheckCommand extends AbstractCommand {

    public CheckCommand(Main main) {
        super(main);
    }

    @Override
    public List<String> tabComplete(String label, String[] args) {
        return null;
    }
    
    @Override
    public void execute(CommandSender sender, String label, String[] args) throws CommandException {
        if (args.length == 0) {
            if (sender instanceof Player) {
                main.getDB().check(((Player)sender).getUniqueId(), 
                        (result) -> {
                            processResults(sender, sender.getName(), result, true);
                        }, (ex) -> {
                            sender.sendMessage(main.getMessage("error"));
                            main.getLogger().log(Level.WARNING, "Error occurred processing check", ex);
                        });
            } else {
                throw new InvalidSenderException();
            }
        } else if (args.length == 1) {
            main.getDB().check(args[0],
                    (result) -> {
                        processResults(sender, args[0], result, false);
                    }, (ex) -> {
                        sender.sendMessage(main.getMessage("error"));
                        main.getLogger().log(Level.WARNING, "Error occurred processing check", ex);
                    });
        } else {
            throw new UsageException();
        }
    }

    private void processResults(CommandSender sender, String name, Map<UUID, Map<String,Long>> results, boolean self) {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String,Long>> e : results.entrySet()) {
            if (!self) {
                sender.sendMessage(main.getMessage("check.account", e.getKey()));
            }
            sender.sendMessage(main.getMessage("check.header"));
            long total = 0L;
            for (Map.Entry<String,Long> ee : e.getValue().entrySet()) {
                sender.sendMessage(main.getMessage("check.row", ee.getKey(), getFormattedTime(now - ee.getValue() )));
                total += now - ee.getValue();
            }
            sender.sendMessage(main.getMessage("check.summary", getFormattedTime(total)));
            Map<String, Main.AutorankResult> performAutorank = main.performAutorank(e.getKey(), e.getValue());
            for (Map.Entry<String, Main.AutorankResult> ee : performAutorank.entrySet()) {
                String promotionName = ee.getKey();
                switch (ee.getValue().getStatus()) {
                    case SUCCESS:
                        if (self) {
                            sender.sendMessage(main.getMessage("check.promote.self", promotionName));
                        } else {
                            sender.sendMessage(main.getMessage("check.promote.other", name, promotionName));
                        }
                        break;
                    case WAIT:
                        if (self) {
                            sender.sendMessage(main.getMessage("check.wait.self", promotionName, getFormattedTime(ee.getValue().getWait())));
                        } else {
                            sender.sendMessage(main.getMessage("check.wait.other", name, promotionName, getFormattedTime(ee.getValue().getWait()) ));
                        }
                        break;
                }
            }
        }
    }
}