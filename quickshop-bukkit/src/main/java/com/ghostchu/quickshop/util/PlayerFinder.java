/*
 *  This file is a part of project QuickShop, the name is PlayerFinder.java
 *  Copyright (C) Ghost_chu and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ghostchu.quickshop.util;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.enginehub.squirrelid.Profile;
import org.enginehub.squirrelid.cache.HashMapCache;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A player finder for finding player by name or unique id.
 *
 * @author Ghost_chu
 * @since Hikari-1.0.3, refactored
 */
public class PlayerFinder {
    private final ProfileService resolver;
    private final ProfileCache cache;

    public PlayerFinder() {
        ProfileCache cache = new HashMapCache(); // Memory cache
        try {
            cache = new SQLiteCache(new File(Util.getCacheFolder(), "player_mapping.db"));
        } catch (Exception e) {
            Util.debugLog("Failed to initialize player mapping cache database, use HashMapCache instead.");
        }
        List<ProfileService> services = new ArrayList<>();
        if(PaperLib.isPaper() && !System.getProperties().containsKey("com.ghostchu.quickshop.util.PlayerFinder.forceSpigot")){
            services.add(PaperPlayerService.getInstance());
        }else {
            Util.debugLog("Fallback to use general CombinedProfileService for player lookup.");
            services.add(new CacheForwardingService(new ParallelProfileService(HttpRepositoryService.forMinecraft(), 2), cache));
        }
        this.resolver = new CombinedProfileService(services);
        this.cache = cache;
    }

    public boolean contains(@NotNull UUID uuid) {
        return cache.getIfPresent(uuid) != null;
    }

    public void flash(@NotNull UUID uuid, @NotNull String name) {
        this.cache.put(new Profile(uuid, name));
    }

    @Nullable
    public Profile find(@NotNull UUID uuid) {
        if (Bukkit.getServer().getOnlineMode()) {
            return findOnline(uuid);
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        if (name == null)
            return null;
        return new Profile(offlinePlayer.getUniqueId(), name);
    }

    @Nullable
    public Profile find(@NotNull String name) {
        // Fallback to UUID lookup if name is UUID.
        if (Util.isUUID(name)) {
            return find(UUID.fromString(name));
        }
        if (Bukkit.getServer().getOnlineMode()) {
            return findOnline(name);
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline())
            return null;
        return new Profile(offlinePlayer.getUniqueId(), name);
    }

    @Nullable
    private Profile findOnline(@NotNull UUID uuid) {
        try {
            return this.resolver.findByUuid(uuid);
        } catch (IOException | InterruptedException e) {
            Util.debugLog("Failed to find player profile: " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private Profile findOnline(@NotNull String name) {
        try {
            return this.resolver.findByName(name);
        } catch (IOException | InterruptedException e) {
            Util.debugLog("Failed to find player profile: " + e.getMessage());
            return null;
        }
    }
}
