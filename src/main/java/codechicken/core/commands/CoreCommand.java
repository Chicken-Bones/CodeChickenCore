package codechicken.core.commands;

import java.util.List;

import codechicken.core.ServerUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public abstract class CoreCommand implements ICommand
{
    public class WCommandSender implements ICommandSender
    {
        public ICommandSender wrapped;

        public WCommandSender(ICommandSender sender) {
            wrapped = sender;
        }

        @Override
        public String getCommandSenderName() {
            return wrapped.getCommandSenderName();
        }

        @Override
        public void addChatMessage(IChatComponent string) {
            wrapped.addChatMessage(string);
        }

        @Override
        public boolean canCommandSenderUseCommand(int i, String s) {
            return wrapped.canCommandSenderUseCommand(i, s);
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return wrapped.getPlayerCoordinates();
        }

        @Override
        public World getEntityWorld() {
            return wrapped.getEntityWorld();
        }

        @Override
        public IChatComponent func_145748_c_() {
            return new ChatComponentText(getCommandSenderName());
        }

        public void chatT(String s, Object... params) {
            addChatMessage(new ChatComponentTranslation(s, params));
        }

        public void chatOpsT(String s, Object... params) {
            for (EntityPlayerMP player : ServerUtils.getPlayers())
                if (MinecraftServer.getServer().getConfigurationManager().func_152596_g(player.getGameProfile()))
                    player.addChatMessage(new ChatComponentTranslation(s, params));
        }
    }

    public abstract boolean OPOnly();

    @Override
    public String getCommandUsage(ICommandSender var1) {
        return "/" + getCommandName() + " help";
    }

    @Override
    public void processCommand(ICommandSender listener, String[] args) {
        WCommandSender wsender = new WCommandSender(listener);

        if (args.length < minimumParameters() ||
                args.length == 1 && args[0].equals("help")) {
            printHelp(wsender);
            return;
        }

        String command = getCommandName();
        for (String arg : args)
            command += " " + arg;

        handleCommand(command, wsender.getCommandSenderName(), args, wsender);
    }

    public abstract void handleCommand(String command, String playername, String[] args, WCommandSender listener);

    public abstract void printHelp(WCommandSender listener);

    public final EntityPlayerMP getPlayer(String name) {
        return ServerUtils.getPlayer(name);
    }

    public WorldServer getWorld(int dimension) {
        return DimensionManager.getWorld(dimension);
    }

    public WorldServer getWorld(EntityPlayer player) {
        return (WorldServer) player.worldObj;
    }

    @Override
    public int compareTo(Object arg0) {
        return getCommandName().compareTo(((ICommand) arg0).getCommandName());
    }

    @Override
    public List<?> getCommandAliases() {
        return null;
    }

    @Override
    public List<?> addTabCompletionOptions(ICommandSender var1, String[] var2) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] astring, int i) {
        return false;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender var1) {
        if (OPOnly()) {
            if (var1 instanceof EntityPlayer)
                return MinecraftServer.getServer().getConfigurationManager().func_152596_g(((EntityPlayer) var1).getGameProfile());
            else if (var1 instanceof MinecraftServer)
                return true;
            else
                return false;
        }
        return true;
    }


    public abstract int minimumParameters();
}
