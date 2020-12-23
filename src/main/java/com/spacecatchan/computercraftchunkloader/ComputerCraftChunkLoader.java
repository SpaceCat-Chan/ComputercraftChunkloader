package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod(modid = ComputerCraftChunkLoader.MODID, name = ComputerCraftChunkLoader.NAME, version = ComputerCraftChunkLoader.VERSION)
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
        // some example code
        logger.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
        ChunkLoader.instance = this;
        loader = new ChunkLoader();
        ForgeChunkManager.setForcedChunkLoadingCallback(this, loader);
    }
    public static boolean debuggerReleaseControl() {
        org.lwjgl.input.Mouse.setGrabbed(false);
        return true;
    }
    @SubscribeEvent
    static public void place(BlockEvent.EntityPlaceEvent event)
    {
        if(event.getWorld().isRemote)
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
            ComputerHandler.AddComputer(event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            ComputerHandler.AddTurtle(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    static public void destroy(BlockEvent.BreakEvent event)
    {
        if(event.getWorld().isRemote)
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
            ComputerHandler.RemoveComputer(event.getWorld(), event.getPos());
        }
        if(is_turtle)
        {
            ComputerHandler.RemoveTurtle(event.getWorld(), event.getPos());
        }
    }

    @SubscribeEvent
    static public void turtle_moved(TurtleActionEvent event)
    {
        if(event.getTurtle().getWorld().isRemote)
        {
            return;
        }
        if(event.getAction() == TurtleAction.MOVE || event.getAction() == TurtleAction.TURN)
        {
            ComputerHandler.UpdateTurtle(event.getTurtle());
        }
    }
}

class ComputerHandler {
    static private ChunkLoader loader;

    public static void setLoader(ChunkLoader _loader) {
        loader = _loader;
    }

    public static void AddComputer(World dim, BlockPos computer_pos)
    {
        m_computers.add(new Tuple<>(dim, computer_pos));
        loader.LoadChunk(computer_pos, dim);
    }

    public static void RemoveComputer(World dim, BlockPos computer_pos)
    {
        m_computers.remove(new Tuple<>(dim, computer_pos));
        loader.UnloadChunk(computer_pos, dim);
    }

    public static void AddTurtle(World dim, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dim);
        loader.LoadChunk(turtle.getPosition(), dim);
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            loader.LoadChunk(turtle.getPosition().offset(direction, 16), dim);
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void UpdateTurtle(dan200.computercraft.api.turtle.ITurtleAccess turtle)
    {
        Pos last_position = m_turtles.get(turtle);

        loader.LoadChunk(turtle.getPosition(), turtle.getWorld());
        if (last_position != null) {
            loader.UnloadChunk(last_position.pos, last_position.world);
        }
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            loader.LoadChunk(turtle.getPosition().offset(direction, 16), turtle.getWorld());
            if (last_position != null) {
                loader.UnloadChunk(last_position.pos.offset(direction, 16), last_position.world);
            }
        }
        m_turtles.put(turtle, new Pos(turtle));
    }

    public static void RemoveTurtle(World dimension, BlockPos turtle_loc)
    {
        ITurtleAccess turtle = TurtleAccessGainer.findAccess(turtle_loc, dimension);
        Pos last_position = m_turtles.get(turtle);
        loader.UnloadChunk(last_position.pos, last_position.world);
        for(EnumFacing direction : EnumFacing.HORIZONTALS) {
            loader.UnloadChunk(last_position.pos.offset(direction, 16), last_position.world);
        }
        m_turtles.remove(turtle);
    }

    public static Set<Tuple<World, BlockPos>> m_computers = new HashSet<>();
    public static Map<ITurtleAccess, Pos> m_turtles = new HashMap<>();
}

class Pos
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
}

class WorldTicket
{
    Map<ChunkPos, Integer> Chunks = new HashMap<>();
    ForgeChunkManager.Ticket ticket;
}

class ChunkLoader implements ForgeChunkManager.LoadingCallback
{
    public static void LoadChunk(BlockPos pos, World dim)
    {
        ChunkPos chunk_pos = new ChunkPos(pos);
        //ComputerCraftChunkLoader.logger.info("received chunk load request at {}, {}", pos, chunk_pos);
        m_loaded_state.putIfAbsent(dim, new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim);
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
        m_loaded_state.putIfAbsent(dim, new WorldTicket());
        WorldTicket world = m_loaded_state.get(dim);
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
    public static Map<World, WorldTicket> m_loaded_state = new HashMap<>();

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world) {
        ForgeChunkManager.Ticket ticket = tickets.get(0);
        m_loaded_state.putIfAbsent(world, new WorldTicket());
        m_loaded_state.get(world).ticket = ticket;
    }
}