package codechicken.core;

import java.util.ArrayList;
import java.util.List;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.command.CommandHandler;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.command.ICommand;
import net.minecraft.util.IChatComponent;

public class ServerUtils extends CommonUtils
{
    public static MinecraftServer mc() {
        return MinecraftServer.getServer();
    }

    public static EntityPlayerMP getPlayer(String playername) {
        return mc().getConfigurationManager().func_152612_a(playername);
    }

    public static List<EntityPlayerMP> getPlayers() {
        return mc().getConfigurationManager().playerEntityList;
    }

    public static ArrayList<EntityPlayer> getPlayersInDimension(int dimension) {
        ArrayList<EntityPlayer> players = new ArrayList<EntityPlayer>();
        for (EntityPlayer p : getPlayers())
            if(p.dimension == dimension)
                players.add(p);

        return players;
    }

    public static void openSMPContainer(EntityPlayerMP player, Container container, IGuiPacketSender packetSender) {
        player.getNextWindowId();
        player.closeContainer();
        packetSender.sendPacket(player, player.currentWindowId);
        player.openContainer = container;
        player.openContainer.windowId = player.currentWindowId;
        player.openContainer.addCraftingToCrafters(player);
    }

    public static GameProfile getGameProfile(String username) {
        return mc().func_152358_ax().func_152655_a(username);
    }

    public static boolean isPlayerOP(String username) {
        GameProfile prof = getGameProfile(username);
        if(prof == null) return false;
        return mc().getConfigurationManager().func_152596_g(getGameProfile(username));
    }

    public static boolean isPlayerOwner(String username) {
        return mc().isSinglePlayer() && mc().getServerOwner().equalsIgnoreCase(username);
    }

    public static void sendChatToAll(IChatComponent msg) {
        for(EntityPlayer p : getPlayers())
            p.addChatComponentMessage(msg);
    }
}
