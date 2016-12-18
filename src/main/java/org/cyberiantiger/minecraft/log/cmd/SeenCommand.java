/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cyberiantiger.minecraft.log.LastSeen;
import org.cyberiantiger.minecraft.log.Main;
import org.cyberiantiger.minecraft.log.MojangAccount;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

/**
 *
 * @author antony
 */
public class SeenCommand extends AbstractCommand {
    private static final Pattern VALID_USERNAME = Pattern.compile("[a-zA-Z0-9_]{2,16}");
    private Map<CommandSender, Map<MojangAccount, Map<String, LastSeen>>> searchResults;


    public SeenCommand(Main main) {
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
                main.getLogger().log(Level.WARNING, "Error occurred processing seen <name>", ex);
            });
        } else {
            try { 
                // Try previous search results.
                int resultNumber = Integer.parseInt(args[0]);
                Map<MojangAccount, Map<String, LastSeen>> lastResults = searchResults.get(sender);
                if (lastResults == null) {
                    sender.sendMessage(main.getMessage("seen.error.no_results"));
                    return;
                }
                if (resultNumber < 1 || resultNumber > searchResults.size()) {
                    sender.sendMessage(main.getMessage("seen.error.invalid_result_number", lastResults.size()));
                    return;
                }

            } catch (NumberFormatException e) {
                // Assume IP based query.
                if (!sender.hasPermission("seen.where.ip")) {
                    throw new PermissionException("seen.where.ip");
                }
                main.getDB().seenIp(target, (result) -> {
                    processResult(sender, result);
                }, (ex) -> {
                    sender.sendMessage(main.getMessage("error"));
                    main.getLogger().log(Level.WARNING, "Error occurred processing seen <ip>", ex);
                });
            }
        }
    }


    private void processResult(CommandSender sender, Map<MojangAccount, Map<String, LastSeen>> result) {
        if (result.isEmpty()) {
            sender.sendMessage(main.getMessage("seen.error.not_found"));
        } else if (result.size() == 1) {
            Map.Entry<MojangAccount, Map<String, LastSeen>> e = result.entrySet().iterator().next();
            MojangAccount account = e.getKey();
            Map<String, LastSeen> data = e.getValue();
            // TODO Print out a bunch of crap, verbose flag ?
        } else {
            searchResults.put(sender, result);
            sender.sendMessage(main.getMessage("seen.search_header", result.size()));
            int counter = 1;
            for (MojangAccount account : result.keySet()) {
                sender.sendMessage(main.getMessage("seen.search_result", 
                        counter,
                        account.getId(),
                        account.getLastSeen().keySet().stream().collect(Collectors.joining(", "))
                ));
                counter++;
            }
            sender.sendMessage(main.getMessage("seen.search_footer", result.size()));
        }
        /*
            sender.sendMessage(main.getMessage("seen.header", result.size()));
            result.entrySet().stream().forEach((e) -> {
                long now = System.currentTimeMillis();
                boolean f1 = true;
                boolean f2 = true;
                StringBuilder aliases = new StringBuilder();
                for (Map.Entry<String, LastSeen> ee : e.getValue().entrySet()) {
                    if (f1) {
                        f1 = false;
                        Period period = trimPeriod(now - ee.getValue().getTime());
                        sender.sendMessage(
                                main.getMessage("seen.player",
                                        ee.getKey(), // Name
                                        e.getKey().toString() // Uuid
                                ));
                        if (sender.hasPermission("ducklog.seen.ip")) {
                            sender.sendMessage(
                                    main.getMessage("seen.where.ip",
                                            ee.getValue().getServer(), // Server
                                            main.getMessage("action." + ee.getValue().getType().name().toLowerCase()), // Action
                                            ee.getValue().getIp() // Ip
                                    ));
                        } else {
                            sender.sendMessage(
                                    main.getMessage("seen.where.basic",
                                            ee.getValue().getServer(), // Server
                                            main.getMessage("action." + ee.getValue().getType().name().toLowerCase()) // Action
                                    ));
                        }
                        sender.sendMessage(
                                main.getMessage("seen.time",
                                        PeriodFormat.getDefault().print(period) // Time
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
                */
    }
}