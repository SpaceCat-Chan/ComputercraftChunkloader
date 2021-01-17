package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.StringInputStream;

import java.io.*;
import java.util.*;

@Mod(modid = ComputerCraftChunkLoader.MODID, name = ComputerCraftChunkLoader.NAME, version = ComputerCraftChunkLoader.VERSION, acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber(modid = ComputerCraftChunkLoader.MODID)
public class ComputerCraftChunkLoader
{
    public static final String MODID = "computercraftchunkloader";
    public static final String NAME = "ComputercraftChunkloader";
    public static final String VERSION = "0.1";

    public static Logger logger;

    private ChunkLoader loader;

    private static final String[] computer_names = {
            "computercraft:computer"
    };
    private static final String[] turtle_names = {
            "computercraft:turtle_expanded",
            "computercraft:turtle_advanced",
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // some example
        ChunkLoader.instance = this;
        ForgeChunkManager.setForcedChunkLoadingCallback(this, loader);
    }
    public static boolean debuggerReleaseControl() {
        org.lwjgl.input.Mouse.setGrabbed(false);
        return true;
    }
    @SubscribeEvent
    static public void place(BlockEvent.EntityPlaceEvent event)
    {
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
            if(!event.getWorld().isRemote) {
                DataSaver saver = DataSaver.get(event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.AddComputer(event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            if(!event.getWorld().isRemote) {
                DataSaver saver = DataSaver.get(event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.AddTurtle(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    static public void destroy(BlockEvent.BreakEvent event)
    {
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
            if(!event.getWorld().isRemote) {
                DataSaver saver = DataSaver.get(event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.RemoveComputer(event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            if(!event.getWorld().isRemote) {
                DataSaver saver = DataSaver.get(event.getWorld());
                saver.markDirty();
            }
            ComputerHandler.RemoveTurtle(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    static public void turtle_moved(TurtleActionEvent event)
    {
        if(event.getAction() == TurtleAction.MOVE)
        {
            if(!event.getTurtle().getWorld().isRemote) {
                DataSaver saver = DataSaver.get(event.getTurtle().getWorld());
                saver.markDirty();
            }
            ComputerHandler.UpdateTurtle(event.getTurtle());
        }
    }
}

class ComputerHandler {

    public static void AddComputer(World dim, BlockPos computer_pos)
    {
        ChunkLoader.LoadChunk(computer_pos, dim);
    }

    public static void RemoveComputer(World dim, BlockPos computer_pos)
    {
        ChunkLoader.UnloadChunk(computer_pos, dim);
    }

    public static void AddTurtle(World dim, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dim);
        ChunkLoader.LoadChunk(turtle.getPosition(), dim);
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            ChunkLoader.LoadChunk(turtle.getPosition().offset(direction, 16), dim);
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void UpdateTurtle(dan200.computercraft.api.turtle.ITurtleAccess turtle)
    {
        Pos last_position = m_turtles.get(turtle);

        ChunkLoader.LoadChunk(turtle.getPosition(), turtle.getWorld());
        if (last_position != null) {
            ChunkLoader.UnloadChunk(last_position.pos, last_position.world);
        }
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            ChunkLoader.LoadChunk(turtle.getPosition().offset(direction, 16), turtle.getWorld());
            if (last_position != null) {
                ChunkLoader.UnloadChunk(last_position.pos.offset(direction, 16), last_position.world);
            }
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void RemoveTurtle(World dimension, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dimension);
        Pos last_position = m_turtles.get(turtle);
        ChunkLoader.UnloadChunk(last_position.pos, last_position.world);
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            ChunkLoader.UnloadChunk(last_position.pos.offset(direction, 16), last_position.world);
        }
        m_turtles.remove(turtle);
    }

    public static Map<ITurtleAccess, Pos> m_turtles = new HashMap<>();
}

class Pos implements Serializable
{
    public World world;
    public BlockPos pos;
    public EnumFacing direction;

    public Pos(World _world, BlockPos _pos, EnumFacing _direction)
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
        direction = (EnumFacing)in.readObject();
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
    ForgeChunkManager.Ticket ticket;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        Map<ChunkPosWrapper, Integer> new_chunks = new HashMap<ChunkPosWrapper, Integer>();
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

class ChunkLoader implements ForgeChunkManager.LoadingCallback
{
    public static void LoadChunk(BlockPos pos, World dim)
    {
        ChunkPos chunk_pos = new ChunkPos(pos);
        //ComputerCraftChunkLoader.logger.info("received chunk load request at {}, {}", pos, chunk_pos);
        m_loaded_state.putIfAbsent(dim.provider.getDimension(), new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim.provider.getDimension());
        AcquireTicket(world, dim);
        world.Chunks.putIfAbsent(chunk_pos, 0);

        //ComputerCraftChunkLoader.logger.info("pre-ref count for chunk {}", world.Chunks.get(chunk_pos));
        if (world.Chunks.get(chunk_pos) == 0)
        {
            ComputerCraftChunkLoader.logger.info("loading chunk {}", chunk_pos);
            ForgeChunkManager.forceChunk(world.ticket, chunk_pos);
        }
        world.Chunks.put(chunk_pos, world.Chunks.get(chunk_pos)+1);
        //ComputerCraftChunkLoader.logger.info("post-ref count for chunk {}", world.Chunks.get(chunk_pos));
    }

    public static void UnloadChunk(BlockPos pos, World dim)
    {
        ChunkPos chunk_pos = new ChunkPos(pos);
        //ComputerCraftChunkLoader.logger.info("received chunk unload request at {}, {}", pos, chunk_pos);
        m_loaded_state.putIfAbsent(dim.provider.getDimension(), new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim.provider.getDimension());
        AcquireTicket(world, dim);
        world.Chunks.putIfAbsent(chunk_pos, 1);
        //ComputerCraftChunkLoader.logger.info("pre-ref count for chunk {}", world.Chunks.get(chunk_pos));
        world.Chunks.put(chunk_pos, world.Chunks.get(chunk_pos)-1);
        if (world.Chunks.get(chunk_pos) <= 0)
        {
            ComputerCraftChunkLoader.logger.info("unloading chunk {}", chunk_pos);
            ForgeChunkManager.unforceChunk(world.ticket, chunk_pos);
            world.Chunks.put(chunk_pos, 0);
        }
        //ComputerCraftChunkLoader.logger.info("post-ref count for chunk {}", world.Chunks.get(chunk_pos));
        
    }

    private static void AcquireTicket(WorldTicket world, World dim)
    {
        if(world.ticket == null)
        {
            world.ticket = ForgeChunkManager.requestTicket(instance, dim, ForgeChunkManager.Type.NORMAL);
        }
        if(world.ticket == null) {
            ComputerCraftChunkLoader.logger.warn("unable to get ticket for dimension {}", dim.provider.getDimension());
        }
    }

    public static ComputerCraftChunkLoader instance;
    public static Map<Integer, WorldTicket> m_loaded_state = new HashMap<>();

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        ForgeChunkManager.Ticket ticket = tickets.get(0);
        m_loaded_state.putIfAbsent(world.provider.getDimension(), new WorldTicket());
        m_loaded_state.get(world.provider.getDimension()).ticket = ticket;
    }
}
