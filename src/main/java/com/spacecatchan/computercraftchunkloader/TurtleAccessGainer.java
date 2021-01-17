package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.shared.turtle.blocks.ITurtleTile;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TurtleAccessGainer {
	static ITurtleAccess findAccess(@Nonnull BlockPos pos, @Nonnull World world)
	{
		TileEntity tile = world.getTileEntity(pos);
		if(tile instanceof ITurtleTile)
		{
			return ((ITurtleTile) tile).getAccess();
		}
		throw new RuntimeException(String.format("expected turtle at {%f, %f, %f}, but none was found",
				pos.getX(), pos.getY(), pos.getZ()));
	}
}
