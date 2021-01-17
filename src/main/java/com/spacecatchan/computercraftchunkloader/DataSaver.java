package com.spacecatchan.computercraftchunkloader;

import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        out.writeObject(to_write.provider.getDimension());
    }

    public static World readWorld(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        return DimensionManager.getWorld((int)in.readObject());
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

    public static DataSaver get(World world)
    {
        MapStorage storage = world.getMapStorage();
        DataSaver saver = (DataSaver)storage.getOrLoadData(DataSaver.class, NAME);
        if(saver == null)
        {
            saver = new DataSaver(NAME);
            storage.setData(NAME, saver);
            saver.markDirty();
        }
        return saver;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        byte[] value = nbt.getByteArray("data");
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(value));
            readTurtles(input);
            ChunkLoader.m_loaded_state = (Map<Integer, WorldTicket>) input.readObject();
            input.close();
        }
        catch (IOException | ClassNotFoundException e)
        {
            ComputerCraftChunkLoader.logger.error(e.toString());
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
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
        nbt.setByteArray("data", baos.toByteArray());
        return nbt;
    }
}
