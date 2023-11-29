package codechicken.core.asm;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.minecraftforge.common.MinecraftForge;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import codechicken.core.CCUpdateChecker;
import codechicken.core.ClientUtils;
import codechicken.core.featurehack.LiquidTextures;
import codechicken.core.internal.CCCEventHandler;
import codechicken.core.launch.CodeChickenCorePlugin;
import codechicken.lib.config.ConfigFile;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.common.versioning.VersionRange;

public class CodeChickenCoreModContainer extends DummyModContainer {

    public static ConfigFile config;

    public static void loadConfig() {
        if (config == null)
            config = new ConfigFile(new File(CodeChickenCorePlugin.minecraftDir, "config/CodeChickenCore.cfg"))
                    .setComment("CodeChickenCore configuration file.");
    }

    public CodeChickenCoreModContainer() {
        super(getModMetadata());
    }

    private static ModMetadata getModMetadata() {
        final ModMetadata modMetadata = new ModMetadata();
        modMetadata.name = "CodeChicken Core";
        modMetadata.modId = "CodeChickenCore";
        modMetadata.version = Tags.VERSION;
        return modMetadata;
    }

    @Override
    public List<ArtifactVersion> getDependants() {
        LinkedList<ArtifactVersion> deps = new LinkedList<ArtifactVersion>();
        if (!getVersion().contains("$")) {
            deps.add(VersionParser.parseVersionReference("Forge@[10.13.3,)"));
            deps.add(VersionParser.parseVersionReference("NotEnoughItems@[1.0.5,)"));
            deps.add(VersionParser.parseVersionReference("EnderStorage@[1.4.7,)"));
            deps.add(VersionParser.parseVersionReference("ChickenChunks@[1.3.4,)"));
            deps.add(VersionParser.parseVersionReference("Translocator@[1.1.2,)"));
            deps.add(VersionParser.parseVersionReference("WR-CBE|Core@[1.4.1,)"));
        }
        return deps;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide().isClient()) LiquidTextures.init();
    }

    @Subscribe
    public void init(FMLInitializationEvent event) {
        if (event.getSide().isClient()) {
            if (config.getTag("checkUpdates").getBooleanValue(true)) CCUpdateChecker.updateCheck(getModId());

            ClientUtils.enhanceSupportersList("CodeChickenCore");

            FMLCommonHandler.instance().bus().register(new CCCEventHandler());
            MinecraftForge.EVENT_BUS.register(new CCCEventHandler());
        }
    }

    @Override
    public VersionRange acceptableMinecraftVersionRange() {
        return VersionParser.parseRange(CodeChickenCorePlugin.mcVersion);
    }
}
