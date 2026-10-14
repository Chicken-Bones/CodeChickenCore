package codechicken.core.asm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import codechicken.core.launch.CodeChickenCorePlugin;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class DelegatedTransformer implements IClassTransformer {

    private static final ArrayList<IClassTransformer> delegatedTransformers;
    private static final Method m_defineClass;
    private static final Field f_cachedClasses;
    private static IClassTransformer[] transformers;

    static {
        delegatedTransformers = new ArrayList<>();
        try {
            m_defineClass = ClassLoader.class
                    .getDeclaredMethod("defineClass", String.class, byte[].class, Integer.TYPE, Integer.TYPE);
            m_defineClass.setAccessible(true);
            f_cachedClasses = LaunchClassLoader.class.getDeclaredField("cachedClasses");
            f_cachedClasses.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        for (IClassTransformer t : transformers) {
            basicClass = t.transform(name, transformedName, basicClass);
        }
        return basicClass;
    }

    public static void load() {
        scanModsForDelegatedTransformers();
        final int size = delegatedTransformers.size();
        if (size == 0) {
            CodeChickenCorePlugin.logger
                    .debug("No delegated transformer found, skipping registration of main DelegatedTransformer.");
            return;
        }
        CodeChickenCorePlugin.logger
                .debug("Found " + size + " delegated transformers, registering main DelegatedTransformer.");
        transformers = delegatedTransformers.toArray(new IClassTransformer[0]);
        String name = DelegatedTransformer.class.getName();
        FMLRelaunchLog.finer("Registering transformer %s", name);
        Launch.classLoader.registerTransformer(name);
    }

    private static void scanModsForDelegatedTransformers() {
        File modsDir = new File(CodeChickenCorePlugin.minecraftDir, "mods");
        if (modsDir.exists()) {
            final File[] files = modsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanMod(file);
                }
            }
        }
        File versionModsDir = new File(
                CodeChickenCorePlugin.minecraftDir,
                "mods/" + CodeChickenCorePlugin.currentMcVersion);
        if (versionModsDir.exists()) {
            final File[] files = versionModsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanMod(file);
                }
            }
        }
    }

    private static void scanMod(File file) {
        if (!file.getName().endsWith(".jar") && !file.getName().endsWith(".zip")) return;

        try {
            try (JarFile jar = new JarFile(file)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) return;
                Attributes attr = manifest.getMainAttributes();
                if (attr == null) return;

                String transformer = attr.getValue("CCTransformer");
                if (transformer != null) {
                    addTransformer(transformer, jar, file);
                }
            }
        } catch (Exception e) {
            CodeChickenCorePlugin.logger.error("CodeChickenCore: Failed to read jar file: " + file.getName(), e);
        }
    }

    public static void addTransformer(String transformer, JarFile jar, File jarFile) {
        CodeChickenCorePlugin.logger.debug("Adding CCTransformer: " + transformer);
        try {
            byte[] bytes;
            bytes = Launch.classLoader.getClassBytes(transformer);

            if (bytes == null) {
                String resourceName = transformer.replace('.', '/') + ".class";
                ZipEntry entry = jar.getEntry(resourceName);
                if (entry == null) throw new Exception(
                        "Failed to add transformer: " + transformer
                                + ". Entry not found in jar file "
                                + jarFile.getName());

                bytes = readFully(jar.getInputStream(entry));
            }

            defineDependancies(bytes, jar, jarFile);
            Class<?> clazz = defineClass(transformer, bytes);

            if (!IClassTransformer.class.isAssignableFrom(clazz)) throw new Exception(
                    "Failed to add transformer: " + transformer + " is not an instance of IClassTransformer");

            IClassTransformer classTransformer;
            try {
                classTransformer = (IClassTransformer) clazz.getDeclaredConstructor(File.class).newInstance(jarFile);
            } catch (NoSuchMethodException nsme) {
                classTransformer = (IClassTransformer) clazz.newInstance();
            }
            delegatedTransformers.add(classTransformer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void defineDependancies(byte[] bytes, JarFile jar, File jarFile) throws Exception {
        defineDependancies(bytes, jar, jarFile, new Stack<String>());
    }

    private static void defineDependancies(byte[] bytes, JarFile jar, File jarFile, Stack<String> depStack)
            throws Exception {
        ClassReader reader = new ClassReader(bytes);
        DependancyLister lister = new DependancyLister(Opcodes.ASM4);
        reader.accept(lister, 0);

        depStack.push(reader.getClassName());

        for (String dependancy : lister.getDependancies()) {
            if (depStack.contains(dependancy)) continue;

            try {
                Launch.classLoader.loadClass(dependancy.replace('/', '.'));
            } catch (ClassNotFoundException cnfe) {
                ZipEntry entry = jar.getEntry(dependancy + ".class");
                if (entry == null)
                    throw new Exception("Dependency " + dependancy + " not found in jar file " + jarFile.getName());

                byte[] depbytes = readFully(jar.getInputStream(entry));
                defineDependancies(depbytes, jar, jarFile, depStack);

                CodeChickenCorePlugin.logger.debug("Defining dependancy: " + dependancy);

                defineClass(dependancy.replace('/', '.'), depbytes);
            }
        }

        depStack.pop();
    }

    private static Class<?> defineClass(String classname, byte[] bytes) throws Exception {
        Class<?> clazz = (Class<?>) m_defineClass.invoke(Launch.classLoader, classname, bytes, 0, bytes.length);
        ((Map<String, Class<?>>) f_cachedClasses.get(Launch.classLoader)).put(classname, clazz);
        return clazz;
    }

    public static byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(stream.available());
        int r;
        while ((r = stream.read()) != -1) {
            bos.write(r);
        }
        return bos.toByteArray();
    }
}
