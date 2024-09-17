/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package love.DingLi.injection.mixins;

import baritone.utils.accessor.IAnvilChunkLoader;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;

/**
 * @author Brady
 * @since 9/4/2018
 */
@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader implements IAnvilChunkLoader {

    @Shadow
    @Final
    public File chunkSaveLocation;

    @Override
    public File getChunkSaveLocation() {
        return this.chunkSaveLocation;
    }
}
