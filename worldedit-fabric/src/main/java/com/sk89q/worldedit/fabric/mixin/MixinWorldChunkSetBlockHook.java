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

package com.sk89q.worldedit.fabric.mixin;

import com.sk89q.worldedit.fabric.internal.ExtendedChunk;
import com.sk89q.worldedit.fabric.internal.OnBlockAddedHelper;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunkSetBlockHook implements Chunk, ExtendedChunk {

    @Shadow @Final private World world;
    private boolean shouldUpdate = true;

    @Nullable
    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved, boolean update) {
        // save the state for the hook
        shouldUpdate = update;
        try {
            return setBlockState(pos, state, moved);
        } finally {
            // restore natural mode
            shouldUpdate = true;
        }
    }

    @Inject(
        method = "setBlockState", require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onBlockAdded(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V", ordinal = 0)
    )
    public void setBlockStateHook(CallbackInfoReturnable<BlockState> cir) {
        boolean localShouldUpdate;
        MinecraftServer server = world.getServer();
        if (server == null || Thread.currentThread() != server.getThread()) {
            // We're not on the server thread for some reason, WorldEdit will never be here
            // so we'll just ignore our flag
            localShouldUpdate = true;
        } else {
            localShouldUpdate = shouldUpdate;
        }
        if (!localShouldUpdate) {
            OnBlockAddedHelper.DISABLED_FLAG.set(true);
        }
    }
}
