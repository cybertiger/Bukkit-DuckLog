/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;
import org.joda.time.format.PeriodFormat;

/**
 *
 * @author antony
 */
public class TimeTopCommand extends AbstractCommand {

    public TimeTopCommand(Main main) {
        super(main);
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) throws CommandException {
        if (args.length == 0) {
            main.getDB().timetop(
                    (result) -> {
                        formatTopTen(sender, result);
                    }, (ex) -> {
                        sender.sendMessage(main.getMessage("error"));
                        main.getLogger().log(Level.WARNING, "Error occurred processing timetop", ex);
                    });
        } else if (args.length == 1) {
            String server = args[0];
            main.getDB().timetop(server,
                    (result) -> {
                        formatTopTen(sender, server, result);
                    }, (ex) -> {
                        sender.sendMessage(main.getMessage("error"));
                        main.getLogger().log(Level.WARNING, "Error occurred processing timetop", ex);
                    });
        } else {
            throw new UsageException();
        }
    }

    @Override
    public List<String> tabComplete(String label, String[] args) {
        // TODO
        return null;
    }

    private void formatTopTen(CommandSender sender, Map<String, Long> top) {
        sender.sendMessage(main.getMessage("topten.all"));
        int position = 1;
        for (Map.Entry<String,Long> e : top.entrySet()) {
            sender.sendMessage(main.getMessage("topten.row", position++, e.getKey(), PeriodFormat.getDefault().print(trimPeriod(e.getValue()))));
        }
    }
    private void formatTopTen(CommandSender sender, String server, Map<String, Long> top) {
        sender.sendMessage(main.getMessage("topten.server", server));
        int position = 1;
        for (Map.Entry<String,Long> e : top.entrySet()) {
            sender.sendMessage(main.getMessage("topten.row", position++, e.getKey(), PeriodFormat.getDefault().print(trimPeriod(e.getValue()))));
        }
    }
}
