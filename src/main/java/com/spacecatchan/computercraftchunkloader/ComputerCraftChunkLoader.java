package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod(ComputerCraftChunkLoader.MODID)
@Mod.EventBusSubscriber(modid = ComputerCraftChunkLoader.MODID)
public class ComputerCraftChunkLoader
{
    public static final String MODID = "computercraftchunkloader";
    public static final String NAME = "ComputercraftChunkloader";
    public static final String VERSION = "0.2";

    public static Logger logger = LogManager.getLogger();

    private ChunkLoader loader = new ChunkLoader();

    private static final String[] computer_names = {
            "computercraft:computer_normal",
            "computercraft:computer_command",
            "computercraft:computer_advanced"
    };
    private static final String[] turtle_names = {
            "computercraft:turtle_normal",
            "computercraft:turtle_advanced"
    };

    public ComputerCraftChunkLoader() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    public void init(FMLCommonSetupEvent event)
    {
        // some example
        ChunkLoader.instance = this;
        ForgeChunkManager.setForcedChunkLoadingCallback(MODID, loader);
    }
    @SubscribeEvent
    public static void place(BlockEvent.EntityPlaceEvent event)
    {
        if(((World)event.getWorld()).isRemote())
        {
            return;
        }
        ResourceLocation BlockName = event.getPlacedBlock().getBlock().getRegistryName();

        boolean is_computer = false;
        boolean is_turtle = false;

        for(String name : computer_names)
        {
            is_computer = is_computer || (BlockName.toString().equals(name));
        }
        for(String name : turtle_names)
        {
            is_turtle = is_turtle || (BlockName.toString().equals(name));
        }

        if(is_computer)
        {
            if(!((World)event.getWorld()).isRemote) {
                DataSaver saver = DataSaver.get((ServerWorld)event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.AddComputer((World)event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            if(!((World)event.getWorld()).isRemote) {
                DataSaver saver = DataSaver.get((ServerWorld)event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.AddTurtle((World)event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void destroy(BlockEvent.BreakEvent event)
    {
        if(((World)event.getWorld()).isRemote())
        {
            return;
        }
        ResourceLocation BlockName = event.getState().getBlock().getRegistryName();

        boolean is_computer = false;
        boolean is_turtle = false;

        for(String name : computer_names)
        {
            is_computer = is_computer || (BlockName.toString().equals(name));
        }
        for(String name : turtle_names)
        {
            is_turtle = is_turtle || (BlockName.toString().equals(name));
        }

        if(is_computer)
        {
            if(!((World)event.getWorld()).isRemote) {
                DataSaver saver = DataSaver.get((ServerWorld)event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.RemoveComputer((World)event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            if(!((World)event.getWorld()).isRemote) {
                DataSaver saver = DataSaver.get((ServerWorld)event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.RemoveTurtle((World)event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void turtle_moved(TurtleActionEvent event)
    {
        if(((World)event.getTurtle().getWorld()).isRemote())
        {
            return;
        }
        if(event.getAction() == TurtleAction.MOVE)
        {
            if(!event.getTurtle().getWorld().isRemote) {
                DataSaver saver = DataSaver.get((ServerWorld)event.getTurtle().getWorld());
                saver.markDirty();
            }
            ComputerHandler.UpdateTurtle(event.getTurtle());
        }
    }
}

class ComputerHandler {

    public static void AddComputer(World dim, BlockPos computer_pos)
    {
        ChunkLoader.LoadChunk(computer_pos, (ServerWorld)dim, computer_pos);
    }

    public static void RemoveComputer(World dim, BlockPos computer_pos)
    {
        ChunkLoader.UnloadChunk(computer_pos, (ServerWorld)dim, computer_pos);
    }

    public static void AddTurtle(World dim, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dim);
        ChunkLoader.LoadChunk(turtle.getPosition(), (ServerWorld)dim, turtle_loc);
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            ChunkLoader.LoadChunk(turtle.getPosition().offset(direction, 16), (ServerWorld)dim, turtle_loc);
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void UpdateTurtle(dan200.computercraft.api.turtle.ITurtleAccess turtle)
    {
        Pos last_position = m_turtles.get(turtle);

        ChunkLoader.LoadChunk(turtle.getPosition(), (ServerWorld)turtle.getWorld(), turtle.getPosition());
        if (last_position != null) {
            ChunkLoader.UnloadChunk(last_position.pos, (ServerWorld)last_position.world, last_position.pos);
        }
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            ChunkLoader.LoadChunk(turtle.getPosition().offset(direction, 16), (ServerWorld)turtle.getWorld(), turtle.getPosition());
            if (last_position != null) {
                ChunkLoader.UnloadChunk(last_position.pos.offset(direction, 16), (ServerWorld)last_position.world, last_position.pos);
            }
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void RemoveTurtle(World dimension, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dimension);
        Pos last_position = m_turtles.get(turtle);
        if(last_position != null) {
	        ChunkLoader.UnloadChunk(last_position.pos, (ServerWorld)last_position.world, last_position.pos);
	        for (Direction direction : Direction.Plane.HORIZONTAL) {
		        ChunkLoader.UnloadChunk(last_position.pos.offset(direction, 16), (ServerWorld)last_position.world, last_position.pos);
	        }
	        m_turtles.remove(turtle);
        }
    }

    public static Map<ITurtleAccess, Pos> m_turtles = new HashMap<>();
}

class Pos implements Serializable
{
    public World world;
    public BlockPos pos;
    public Direction direction;

    public Pos(World _world, BlockPos _pos, Direction _direction)
    {
        world = _world;
        pos = _pos;
        direction = _direction;
    }

    public Pos(ITurtleAccess turtle)
    {
        world = turtle.getWorld();
        pos = turtle.getPosition();
        direction = turtle.getDirection();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        DataSaver.writeWorld(world, out);
        out.writeInt(pos.getX());
        out.writeInt(pos.getY());
        out.writeInt(pos.getZ());
        out.writeObject(direction);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        world = DataSaver.readWorld(in);
        int X = in.readInt();
        int Y = in.readInt();
        int Z = in.readInt();
        pos = new BlockPos(X, Y, Z);
        direction = (Direction)in.readObject();
    }
}

class ChunkPosWrapper implements Serializable
{
    public int x, z;
    public ChunkPosWrapper(ChunkPos a)
    {
        x = a.x;
        z = a.z;
    }
}

class WorldTicket implements Serializable
{
    Map<ChunkPos, Integer> Chunks = new HashMap<>();

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        Map<ChunkPosWrapper, Integer> new_chunks = new HashMap<>();
        for(Map.Entry<ChunkPos, Integer> a : Chunks.entrySet())
        {
            new_chunks.put(new ChunkPosWrapper(a.getKey()), a.getValue());
        }
        out.writeObject(new_chunks);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Map<ChunkPosWrapper, Integer> a = (Map<ChunkPosWrapper, Integer>)in.readObject();
        Chunks = new HashMap<>();
        for(Map.Entry<ChunkPosWrapper, Integer> b : a.entrySet())
        {
            Chunks.put(new ChunkPos(b.getKey().x, b.getKey().z), b.getValue());
        }
    }
}

class ChunkLoader implements ForgeChunkManager.LoadingValidationCallback
{
    public static void LoadChunk(BlockPos pos, ServerWorld dim, BlockPos owner)
    {
        ChunkPos chunk_pos = new ChunkPos(pos);
        //ComputerCraftChunkLoader.logger.info("received chunk load request at {}, {}", pos, chunk_pos);
        m_loaded_state.putIfAbsent(dim.getDimensionKey().getLocation().getPath(), new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim.getDimensionKey().getLocation().getPath());
        world.Chunks.putIfAbsent(chunk_pos, 0);

        //ComputerCraftChunkLoader.logger.info("pre-ref count for chunk {}", world.Chunks.get(chunk_pos));
        if (world.Chunks.get(chunk_pos) == 0)
        {
            ComputerCraftChunkLoader.logger.info("loading chunk {} in {}", chunk_pos, dim.getDimensionKey().getLocation().getPath());
            ForgeChunkManager.forceChunk(dim, ComputerCraftChunkLoader.MODID, owner, chunk_pos.x, chunk_pos.z, true, true);
        }
        world.Chunks.put(chunk_pos, world.Chunks.get(chunk_pos)+1);
        //ComputerCraftChunkLoader.logger.info("post-ref count for chunk {}", world.Chunks.get(chunk_pos));
    }

    public static void UnloadChunk(BlockPos pos, ServerWorld dim, BlockPos owner)
    {
        ChunkPos chunk_pos = new ChunkPos(pos);
        //ComputerCraftChunkLoader.logger.info("received chunk unload request at {}, {}", pos, chunk_pos);
        m_loaded_state.putIfAbsent(dim.getDimensionKey().getLocation().getPath(), new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim.getDimensionKey().getLocation().getPath());
        world.Chunks.putIfAbsent(chunk_pos, 1);
        //ComputerCraftChunkLoader.logger.info("pre-ref count for chunk {}", world.Chunks.get(chunk_pos));
        world.Chunks.put(chunk_pos, world.Chunks.get(chunk_pos)-1);
        if (world.Chunks.get(chunk_pos) <= 0)
        {
            ComputerCraftChunkLoader.logger.info("unloading chunk {} in {}", chunk_pos, dim.getDimensionKey().getLocation().getPath());
            ForgeChunkManager.forceChunk(dim, ComputerCraftChunkLoader.MODID, owner, chunk_pos.x, chunk_pos.z, false, true);
            world.Chunks.put(chunk_pos, 0);
        }
        //ComputerCraftChunkLoader.logger.info("post-ref count for chunk {}", world.Chunks.get(chunk_pos));
        
    }

    public static ComputerCraftChunkLoader instance;
    public static Map<String, WorldTicket> m_loaded_state = new HashMap<>();

    @Override
    public void validateTickets(ServerWorld world, ForgeChunkManager.TicketHelper helper) {

    }
}
