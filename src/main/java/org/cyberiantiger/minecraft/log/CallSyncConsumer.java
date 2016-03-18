/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log;

import java.util.function.Consumer;
import org.bukkit.Bukkit;

/**
 *
 * @author antony
 */
public class CallSyncConsumer<T> implements Consumer<T> {
    private final Main main;
    private final Consumer<T> result;

    public CallSyncConsumer(Main main, Consumer<T> result) {
        this.main = main;
        this.result = result;
    }

    @Override
    public void accept(T t) {
        if (Bukkit.isPrimaryThread()) {
            result.accept(t);
        } else {
            main.getServer().getScheduler().runTask(main, () -> {
                result.accept(t);
            });
        }
    }
    
}
