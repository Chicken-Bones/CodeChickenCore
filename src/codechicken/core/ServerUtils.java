package codechicken.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import codechicken.lib.asm.ObfMapping;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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

    private static Field field_152661_c;
    private static Class<?> c_ProfileEntry;
    private static Method func_152668_a;
    static {
        try {
            field_152661_c = ReflectionManager.getField(new ObfMapping("net/minecraft/server/management/PlayerProfileCache", "field_152661_c", "[Ljava/util/Map;"));
            c_ProfileEntry = ServerUtils.class.getClassLoader().loadClass("net.minecraft.server.management.PlayerProfileCache$ProfileEntry");
            func_152668_a = c_ProfileEntry.getDeclaredMethod(
                    new ObfMapping("net/minecraft/server/management/PlayerProfileCache$ProfileEntry",
                            "func_152668_a", "()Lcom/mojang/authlib/GameProfile;").toRuntime().s_name);
            func_152668_a.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static GameProfile getGameProfile(String username) {
        EntityPlayer player = getPlayer(username);
        if(player != null)
            return player.getGameProfile();

        username = username.toLowerCase(Locale.ROOT);
        try {//use reflection to bypass saving the game profiles every time we ask the cache for one
            Object cacheEntry = ((Map) field_152661_c.get(mc().func_152358_ax())).get(username);
            if(cacheEntry != null)
                return (GameProfile) func_152668_a.invoke(cacheEntry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mc().func_152358_ax().func_152655_a(username);
    }

    public static boolean isPlayerOP(String username) {
        GameProfile prof = getGameProfile(username);
        return prof != null && mc().getConfigurationManager().func_152596_g(prof);
    }

    public static boolean isPlayerOwner(String username) {
        return mc().isSinglePlayer() && mc().getServerOwner().equalsIgnoreCase(username);
    }

    public static void sendChatToAll(IChatComponent msg) {
        for(EntityPlayer p : getPlayers())
            p.addChatComponentMessage(msg);
    }
}
