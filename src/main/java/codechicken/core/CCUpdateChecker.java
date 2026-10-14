package codechicken.core;

import com.google.common.base.Function;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class CCUpdateChecker {

    public static void tick() {}

    public static void addUpdateMessage(String s) {}

    public static String mcVersion() {
        return (String) FMLInjectionData.data()[4];
    }

    public static void updateCheck(final String mod, final String version) {}

    public static void updateCheck(String mod) {}

    public static void updateCheck(String url, Function<String, Void> handler) {}
}
