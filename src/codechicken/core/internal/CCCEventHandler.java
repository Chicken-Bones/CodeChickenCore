package codechicken.core.internal;

import codechicken.core.CCUpdateChecker;

import codechicken.core.GuiModListScroll;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.GuiScreenEvent;

public class CCCEventHandler
{
    public static int renderTime;
    public static float renderFrame;

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if(event.phase == Phase.END) {
            CCUpdateChecker.tick();
            renderTime++;
        }
    }

    @SubscribeEvent
    public void renderTick(TickEvent.RenderTickEvent event) {
        if(event.phase == Phase.START)
            renderFrame = event.renderTickTime;
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void posGuiRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if(event.gui instanceof GuiModList)
            GuiModListScroll.draw((GuiModList)event.gui, event.mouseX, event.mouseY);
    }
}
