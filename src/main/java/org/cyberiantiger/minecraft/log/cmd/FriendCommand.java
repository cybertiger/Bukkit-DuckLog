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
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.LoginEvent;
import org.cyberiantiger.minecraft.log.Main;
import org.joda.time.Period;

/**
 *
 * @author antony & Jabelpeeps
 */
public class FriendCommand extends AbstractCommand {
    private static final Pattern VALID_USERNAME = Pattern.compile("[a-zA-Z0-9_]{2,16}");

    public FriendCommand(Main main) {
        super(main);
    }

    @Override
    public List<String> tabComplete(String label, String[] args) {
        return null;
    }

    @Override
    public void execute(final CommandSender sender, String label, String[] args) throws CommandException {
        if (args.length != 1) throw new UsageException();
        String target = args[0];

        if (VALID_USERNAME.matcher(target).matches()) {
            // Name based query.
            main.getDB().seenName(target, (result) -> {
                processResult(sender, result);
            }, (ex) -> {
                sender.sendMessage(main.getMessage("error"));
                main.getLogger().log(Level.WARNING, "Error occurred processing /friend <name>", ex);
            });
        } 
    }

    private void processResult(CommandSender sender, Map<UUID, Map<String, LoginEvent>> result) {
        if (result.isEmpty()) {
            sender.sendMessage(main.getMessage("friend.not_found"));
        } else {
            result.entrySet().stream().forEach((e) -> {
                long now = System.currentTimeMillis();
                boolean f1 = true;
                boolean f2 = true;
                StringBuilder aliases = new StringBuilder();
                for (Map.Entry<String, LoginEvent> ee : e.getValue().entrySet()) {
                    if (f1) {
                        f1 = false;
                        Period period = trimPeriod(now - ee.getValue().getTime());
                        sender.sendMessage(
                                main.getMessage("friend.player",
                                        ee.getKey(), // Name
                                        ee.getValue().getServer(), // Server
                                        myFormatter().print(period) // Time
                                ));
                    } else {
                        if (f2) {
                            aliases.append(ee.getKey());
                            f2 = false;
                        } else {
                            aliases.append(", ");
                        }
                    }
                    if (e.getValue().size() > 1) {
                        sender.sendMessage(main.getMessage("seen.alts", aliases.toString()));
                        aliases.setLength(0);
                    }
                }
            });
        }
    }
}
