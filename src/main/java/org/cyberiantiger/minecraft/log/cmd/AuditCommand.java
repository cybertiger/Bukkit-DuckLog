/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cyberiantiger.minecraft.log.LoginEvent;
import org.cyberiantiger.minecraft.log.Main;

/**
 *
 * @author antony
 */
public class AuditCommand extends AbstractCommand {

    public AuditCommand(Main main) {
        super(main);
    }

    @Override
    public List<String> tabComplete(String label, String[] args) {
        return null;
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) throws CommandException {
        if (args.length < 1) throw new UsageException();
        String target = args[0];
        int page = 0;
        if (args.length == 2) {
            try {
                page = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                throw new UsageException();
            }
            if (page < 0) throw new UsageException();
        }
        int recordsPerPage = (sender instanceof Player) ? 10 : 50;
        main.getDB().audit(target, recordsPerPage * page, recordsPerPage,
                (result) -> {
                    processResult(sender, result);
                },
                (ex) -> {
                    sender.sendMessage(main.getMessage("error"));
                    main.getLogger().log(Level.WARNING, "Error occurred processing audit", ex);
                });
    }

    private void processResult(CommandSender sender, Map<UUID, List<LoginEvent>> result) {
        if (result.isEmpty()) {
            sender.sendMessage(main.getMessage("audit.not_found"));
        } else if (result.size() == 1) {
            printLoginEntries(sender, result.entrySet().iterator().next());
        } else {
            sender.sendMessage(main.getMessage("audit.multiple_matches", result.size()));
            result.entrySet().stream().forEach((e) -> {
                printLoginEntries(sender, e);
            });
        }
    }

    private void printLoginEntries(CommandSender sender, Map.Entry<UUID, List<LoginEvent>> e) {
        sender.sendMessage(main.getMessage("audit.account", e.getKey().toString()));
        if (e.getValue().isEmpty()) {
            sender.sendMessage(main.getMessage("audit.no_more_records"));
        } else {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            e.getValue().stream().forEach((login) -> {
                sender.sendMessage(main.getMessage("audit.record", 
                        main.getMessage("history." + login.getType().name().toLowerCase()),
                        login.getServer(),
                        login.getIp(),
                        format.format(new Date(login.getTime()))
                ));
            });
        }
    }
}