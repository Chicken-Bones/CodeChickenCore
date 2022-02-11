package codechicken.core.launch;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import codechicken.core.asm.*;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.CoreModManager;

import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@TransformerExclusions(value = {"codechicken.core.asm", "codechicken.obfuscator"})
@MCVersion( "1.7.10" )
public class CodeChickenCorePlugin implements IFMLLoadingPlugin, IFMLCallHook
{
    public static final String mcVersion = "[1.7.10]";
    public static final String version = "GRADLETOKEN_VERSION";

    public static File minecraftDir;
    public static String currentMcVersion;
    public static Logger logger = LogManager.getLogger("CodeChickenCore");

    public CodeChickenCorePlugin() {
        if (minecraftDir != null)
            return;//get called twice, once for IFMLCallHook

        minecraftDir = (File) FMLInjectionData.data()[6];
        currentMcVersion = (String) FMLInjectionData.data()[4];

        DepLoader.load();
        injectDeobfPlugin();
    }

    private void injectDeobfPlugin() {
        try {
            Class<?> wrapperClass = Class.forName("cpw.mods.fml.relauncher.CoreModManager$FMLPluginWrapper");
            Constructor wrapperConstructor = wrapperClass.getConstructor(String.class, IFMLLoadingPlugin.class, File.class, Integer.TYPE, String[].class);
            Field f_loadPlugins = CoreModManager.class.getDeclaredField("loadPlugins");
            wrapperConstructor.setAccessible(true);
            f_loadPlugins.setAccessible(true);
            ((List)f_loadPlugins.get(null)).add(2, wrapperConstructor.newInstance("CCCDeobfPlugin", new MCPDeobfuscationTransformer.LoadPlugin(), null, 0, new String[0]));
        } catch (Exception e) {
            logger.error("Failed to inject MCPDeobfuscation Transformer", e);
        }
    }

    public static void versionCheck(String reqVersion, String mod) {
        String mcVersion = (String) FMLInjectionData.data()[4];
        if (!VersionParser.parseRange(reqVersion).containsVersion(new DefaultArtifactVersion(mcVersion))) {
            String err = "This version of " + mod + " does not support minecraft version " + mcVersion;
            logger.error(err);

            JEditorPane ep = new JEditorPane("text/html",
                    "<html>" +
                            err +
                            "<br>Remove it from your coremods folder and check <a href=\"http://www.minecraftforum.net/topic/909223-\">here</a> for updates" +
                            "</html>");

            ep.setEditable(false);
            ep.setOpaque(false);
            ep.addHyperlinkListener(new HyperlinkListener()
            {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    try {
                        if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                            Desktop.getDesktop().browse(event.getURL().toURI());
                    } catch (Exception ignored) {}
                }
            });

            JOptionPane.showMessageDialog(null, ep, "Fatal error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static long parseSize(String text) {
        double d = Double.parseDouble(text.replaceAll("[GMK]B?$", ""));
        long l = Math.round(d * 1024 * 1024 * 1024L);
        switch (text.replaceAll("\\d?","").toUpperCase().charAt(0)) {
            default:  l /= 1024;
            case 'K': l /= 1024;
            case 'M': l /= 1024;
            case 'G': return l;
        }
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %cB", value / 1024.0, ci.current());
    }

    public static void systemCheck(String minRAM) {
        long minBytes = parseSize(minRAM);
        if (Runtime.getRuntime().maxMemory() < minBytes) {
            String err = "You should have at least " + minRAM + " of RAM but you have only allocated " + humanReadableByteCountBin(Runtime.getRuntime().maxMemory()) + ".";
            logger.error(err);

            JEditorPane ep = new JEditorPane("text/html",
                    "<html>" +
                            err +
                            "<br>GTNH seriously won't run without enough RAM. See <a href=\"https://gtnh.miraheze.org/m/3S2\">the Wiki</a> for details." +
                            "<br>Recommended values are between 4GB and 6GB. Check your launcher's JVM arguments." +
                            "</html>");

            ep.setEditable(false);
            ep.setOpaque(false);
            ep.addHyperlinkListener(new HyperlinkListener()
            {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    try {
                        if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                            Desktop.getDesktop().browse(event.getURL().toURI());
                    } catch (Exception ignored) {}
                }
            });

            if (!GraphicsEnvironment.isHeadless())
                JOptionPane.showMessageDialog(null, ep, "lol nope", JOptionPane.ERROR_MESSAGE);
            FMLCommonHandler.instance().exitJava(-98, true);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        versionCheck(mcVersion, "CodeChickenCore");
        return new String[]{
                "codechicken.lib.asm.ClassHeirachyManager",
                "codechicken.core.asm.InterfaceDependancyTransformer",
                "codechicken.core.asm.TweakTransformer",
                "codechicken.core.asm.DelegatedTransformer",
                "codechicken.core.asm.DefaultImplementationTransformer"};
    }

    @Override
    public String getAccessTransformerClass() {
        return "codechicken.core.asm.CodeChickenAccessTransformer";
    }

    @Override
    public String getModContainerClass() {
        return "codechicken.core.asm.CodeChickenCoreModContainer";
    }

    @Override
    public String getSetupClass() {
        return getClass().getName();
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public Void call() {
        CodeChickenCoreModContainer.loadConfig();
        systemCheck(CodeChickenCoreModContainer.config.getTag("minRAM").getValue("3GB"));
        TweakTransformer.load();
        scanCodeChickenMods();

        return null;
    }

    private void scanCodeChickenMods() {
        File modsDir = new File(minecraftDir, "mods");
        for (File file : modsDir.listFiles())
            scanMod(file);
        File versionModsDir = new File(minecraftDir, "mods/" + currentMcVersion);
        if (versionModsDir.exists())
            for (File file : versionModsDir.listFiles())
                scanMod(file);
    }

    private void scanMod(File file) {
        if (!file.getName().endsWith(".jar") && !file.getName().endsWith(".zip"))
            return;

        try {
            JarFile jar = new JarFile(file);
            try {
                Manifest manifest = jar.getManifest();
                if (manifest == null)
                    return;
                Attributes attr = manifest.getMainAttributes();
                if (attr == null)
                    return;

                String transformer = attr.getValue("CCTransformer");
                if (transformer != null)
                    DelegatedTransformer.addTransformer(transformer, jar, file);
            } finally {
                jar.close();
            }
        } catch (Exception e) {
            logger.error("CodeChickenCore: Failed to read jar file: " + file.getName(), e);
        }
    }
}
