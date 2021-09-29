/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.fabric;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.*;
import com.sk89q.worldedit.fabric.internal.ExtendedChunk;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.lifecycle.Lifecycled;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.Registries;
import net.minecraft.SharedConstants;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.WorldChunk;
import org.enginehub.piston.CommandManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

class FabricPlatform extends AbstractPlatform implements MultiUserPlatform {

    private final FabricWorldEdit mod;
    private final FabricDataFixer dataFixer;
    private final Lifecycled<Optional<Watchdog>> watchdog;
    private boolean hookingEvents = false;

    FabricPlatform(FabricWorldEdit mod) {
        this.mod = mod;
        this.dataFixer = new FabricDataFixer(getDataVersion());

        this.watchdog = FabricWorldEdit.LIFECYCLED_SERVER.map(
            server -> server instanceof MinecraftDedicatedServer
                ? Optional.of((Watchdog) server)
                : Optional.empty()
        );
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public Registries getRegistries() {
        return FabricRegistries.getInstance();
    }

    @Override
    public int getDataVersion() {
        return SharedConstants.getGameVersion().getWorldVersion();
    }

    @Override
    public DataFixer getDataFixer() {
        return dataFixer;
    }

    @Override
    public boolean isValidMobType(String type) {
        return Registry.ENTITY_TYPE.containsId(new Identifier(type));
    }

    @Override
    public void reload() {
        getConfiguration().load();
        super.reload();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return -1;
    }

    @Override
    @Nullable
    public Watchdog getWatchdog() {
        return watchdog.value().flatMap(Function.identity()).orElse(null);
    }

    @Override
    public List<? extends World> getWorlds() {
        Iterable<ServerWorld> worlds = FabricWorldEdit.LIFECYCLED_SERVER.valueOrThrow().getWorlds();
        List<World> ret = new ArrayList<>();
        for (ServerWorld world : worlds) {
            ret.add(new FabricWorld(world));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof FabricPlayer) {
            return player;
        } else {
            ServerPlayerEntity entity = FabricWorldEdit.LIFECYCLED_SERVER.valueOrThrow()
                .getPlayerManager().getPlayer(player.getName());
            return entity != null ? new FabricPlayer(entity) : null;
        }
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof FabricWorld) {
            return world;
        } else {
            for (ServerWorld ws : FabricWorldEdit.LIFECYCLED_SERVER.valueOrThrow().getWorlds()) {
                if (ws.getLevelProperties().getLevelName().equals(world.getName())) {
                    return new FabricWorld(ws);
                }
            }

            return null;
        }
    }

    @Override
    public void registerCommands(CommandManager manager) {
        // No-op, we register using Fabric's event
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
        this.hookingEvents = enabled;
    }

    @Override
    public FabricConfiguration getConfiguration() {
        return mod.getConfig();
    }

    @Override
    public String getVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public String getPlatformName() {
        return "Fabric-Official";
    }

    @Override
    public String getPlatformVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.PREFER_OTHERS);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS_NO_MIXIN = Sets.immutableEnumSet(
        SideEffect.VALIDATION,
        SideEffect.ENTITY_AI,
        SideEffect.LIGHTING,
        SideEffect.NEIGHBORS
    );

    private static final Set<SideEffect> SUPPORTED_SIDE_EFFECTS = Sets.immutableEnumSet(
        Iterables.concat(SUPPORTED_SIDE_EFFECTS_NO_MIXIN, Collections.singleton(SideEffect.UPDATE))
    );

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return ExtendedChunk.class.isAssignableFrom(WorldChunk.class)
            ? SUPPORTED_SIDE_EFFECTS
            : SUPPORTED_SIDE_EFFECTS_NO_MIXIN;
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        PlayerManager scm = FabricWorldEdit.LIFECYCLED_SERVER.valueOrThrow().getPlayerManager();
        for (ServerPlayerEntity entity : scm.getPlayerList()) {
            if (entity != null) {
                users.add(new FabricPlayer(entity));
            }
        }
        return users;
    }
}
