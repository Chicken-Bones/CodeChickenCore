package codechicken.core.asm;

import java.io.IOException;
import java.lang.reflect.Field;

import com.google.common.collect.ImmutableBiMap;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

// This AccessTransformer will make all minecraft classes public at runtime in MCP just as they are in modloader.
// You should ONLY use this when you are testing with a mod that relies on runtime publicity and doesn't include
// access transformers. Such mods are doing the wrong thing and should be fixed.
public class CodeChickenAccessTransformer extends AccessTransformer {

    private static final Field f_classNameBiMap;
    @SuppressWarnings("FieldMayBeFinal")
    private static Object emptyMap = ImmutableBiMap.of();

    static {
        try {
            f_classNameBiMap = FMLDeobfuscatingRemapper.class.getDeclaredField("classNameBiMap");
            f_classNameBiMap.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CodeChickenAccessTransformer() throws IOException {}

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        boolean setPublic = name.startsWith("net.minecraft.");
        if (setPublic) setClassMap(name);
        bytes = super.transform(name, transformedName, bytes);
        if (setPublic) restoreClassMap();
        return bytes;
    }

    private static void restoreClassMap() {
        try {
            f_classNameBiMap.set(FMLDeobfuscatingRemapper.INSTANCE, emptyMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setClassMap(String name) {
        try {
            f_classNameBiMap.set(FMLDeobfuscatingRemapper.INSTANCE, ImmutableBiMap.of(name.replace('.', '/'), ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
