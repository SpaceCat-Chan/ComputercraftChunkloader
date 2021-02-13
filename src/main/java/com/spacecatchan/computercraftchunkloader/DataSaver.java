package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DataSaver extends WorldSavedData {
    private static final String NAME = ComputerCraftChunkLoader.MODID + "_data";

    public DataSaver()
    {
        super(NAME);
    }
    public DataSaver(String a)
    {
        super(a);
    }

    public static void writeWorld(World to_write, ObjectOutputStream out) throws IOException
    {
        out.writeObject(to_write.getDimensionKey().getLocation().getPath());
    }

    public static World readWorld(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Set<RegistryKey<World>> worlds = ServerLifecycleHooks.getCurrentServer().func_240770_D_();
        ResourceLocation mine = new ResourceLocation((String)in.readObject());
        for (RegistryKey<World> key : worlds)
        {
            if(key.getLocation().equals(mine))
            {
                return ServerLifecycleHooks.getCurrentServer().getWorld(key);
            }
        }
        //shouldn't be reached, if it is reached we are fucked
        return ServerLifecycleHooks.getCurrentServer().getWorld((RegistryKey<World>)worlds.toArray()[0]);
    }

    public static void writeTurtles(ObjectOutputStream out) throws IOException
    {
        out.writeObject(new HashSet<>(ComputerHandler.m_turtles.values()));
    }

    public static void readTurtles(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Set<Pos> pos_list = (Set<Pos>)in.readObject();
        for (Pos pos : pos_list) {
            ITurtleAccess access = TurtleAccessGainer.findAccess(pos.pos, pos.world);
            ComputerHandler.m_turtles.put(access, pos);
        }
    }

    public static DataSaver get(ServerWorld world)
    {
        DimensionSavedDataManager storage = world.getSavedData();
        DataSaver saver = (DataSaver)storage.getOrCreate(()->{ return new DataSaver();}, NAME);
        if(saver == null)
        {
            saver = new DataSaver(NAME);
            storage.set(saver);
            saver.markDirty();
        }
        return saver;
    }

    @Override
    public void read(CompoundNBT nbt)
    {
        byte[] value = nbt.getByteArray("data");
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(value));
            readTurtles(input);
            ChunkLoader.m_loaded_state = (Map<String, WorldTicket>) input.readObject();
            input.close();
        }
        catch (IOException | ClassNotFoundException e)
        {
            ComputerCraftChunkLoader.logger.error(e.toString());
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            writeTurtles(oos);
            oos.writeObject(ChunkLoader.m_loaded_state);
            oos.close();
        } catch (IOException e) {
            ComputerCraftChunkLoader.logger.error(e.toString());
        }
        nbt.putByteArray("data", baos.toByteArray());
        return nbt;
    }
}
