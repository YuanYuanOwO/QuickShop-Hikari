package com.ghostchu.quickshop.api.registry.builtin.itemexpression.exception;

import com.ghostchu.quickshop.api.registry.builtin.itemexpression.ItemExpressionHandler;
import org.bukkit.plugin.Plugin;

public class PrefixAlreadyRegisteredException extends Exception {
    private final char prefix;
    private final Plugin plugin;
    private final ItemExpressionHandler handler;

    public PrefixAlreadyRegisteredException(char prefix, Plugin pluginInstance, ItemExpressionHandler handler) {
        super("The prefix " + prefix + " already in use, registered by " + pluginInstance.getName() + " with handler " + handler.getClass().getName() + ", pick another one prefix!");
        this.prefix = prefix;
        this.plugin = pluginInstance;
        this.handler = handler;
    }

    public char getPrefix() {
        return prefix;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ItemExpressionHandler getHandler() {
        return handler;
    }
}
