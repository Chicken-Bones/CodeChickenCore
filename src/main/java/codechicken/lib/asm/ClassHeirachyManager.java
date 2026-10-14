package codechicken.lib.asm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

/**
 * This is added as a class transformer if CodeChickenCore is installed. Adding it as a class transformer will speed
 * evaluation up slightly by automatically caching superclasses when they are first loaded.
 */
public class ClassHeirachyManager implements IClassTransformer {

    private static final Map<String, ClassInfo> superclasses = new ConcurrentHashMap<>(1000);

    static {
        superclasses.put("java.lang.Object", ClassInfo.OBJECT);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if (!superclasses.containsKey(transformedName)) {
            declareASM(transformedName, bytes);
        }
        return bytes;
    }

    private static ClassInfo declareASM(String name, byte[] bytes) {
        final ClassReader classReader = new ClassReader(bytes);
        final ClassInfo classInfo = new ClassInfo(classReader);
        superclasses.put(name, classInfo);
        return classInfo;
    }

    private static ClassInfo declareReflection(String name) throws ClassNotFoundException {
        final Class<?> aclass = Class.forName(name);
        final ClassInfo classInfo = new ClassInfo(aclass);
        superclasses.put(name, classInfo);
        return classInfo;
    }

    private static ClassInfo declareClass(String name) {
        try {
            byte[] bytes = Launch.classLoader.getClassBytes(unKey(name));
            if (bytes != null) {
                return declareASM(name, bytes);
            }
        } catch (Exception ignored) {}
        try {
            return declareReflection(name);
        } catch (ClassNotFoundException ignored) {}
        return null;
    }

    private static String toKey(String name) {
        if (ObfMapping.obfuscated) {
            return FMLDeobfuscatingRemapper.INSTANCE.map(name.replace('.', '/')).replace('/', '.');
        }
        return name.replace('/', '.');
    }

    private static String unKey(String name) {
        if (ObfMapping.obfuscated) {
            return FMLDeobfuscatingRemapper.INSTANCE.unmap(name.replace('.', '/')).replace('/', '.');
        }
        return name.replace('/', '.');
    }

    /**
     * @param name       The class in question
     * @param superclass The class being extended
     * @return true if clazz extends, either directly or indirectly, superclass.
     */
    public static boolean classExtends(String name, String superclass) {
        name = toKey(name);
        superclass = toKey(superclass);

        if (name.equals(superclass)) return true;

        ClassInfo classInfo = superclasses.get(name);
        if (classInfo == null) classInfo = declareClass(name);
        if (classInfo == null) return false;

        return classInfo.hasSuper(superclass);
    }

    public static String getSuperClass(String name, boolean runtime) {
        name = toKey(name);

        ClassInfo classInfo = superclasses.get(name);
        if (classInfo == null) classInfo = declareClass(name);
        if (classInfo == null || classInfo.superclass == null) return "java.lang.Object";

        final String s = classInfo.superclass;
        if (!runtime) {
            return FMLDeobfuscatingRemapper.INSTANCE.unmap(s);
        }
        return s;
    }

    private static class ClassInfo {

        public static final ClassInfo OBJECT = new ClassInfo();
        private static final Map<String, String> STRING_POOL = new ConcurrentHashMap<>(256);

        public final String superclass;
        public final String[] interfaces;
        public ClassInfo parent;

        private ClassInfo() {
            this.superclass = null;
            this.interfaces = null;
        }

        public ClassInfo(ClassReader classReader) {
            final String superName = classReader.getSuperName();
            final String[] interfaces = classReader.getInterfaces();

            if (superName == null || "java/lang/Object".equals(superName)) {
                superclass = "java.lang.Object";
                parent = OBJECT;
            } else {
                superclass = pool(toKey(superName));
            }

            if (interfaces == null || interfaces.length == 0) {
                this.interfaces = null;
            } else {
                this.interfaces = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    this.interfaces[i] = pool(toKey(interfaces[i]));
                }
            }
        }

        public ClassInfo(Class<?> aclass) {
            if (aclass.isInterface()) {
                this.superclass = "java.lang.Object";
                this.parent = OBJECT;
            } else {
                final Class<?> superclass = aclass.getSuperclass();
                if (superclass == null || "java.lang.Object".equals(superclass.getName())) {
                    this.superclass = "java.lang.Object";
                    this.parent = OBJECT;
                } else {
                    this.superclass = pool(toKey(superclass.getName()));
                }
            }
            final Class<?>[] interfaces = aclass.getInterfaces();
            if (interfaces.length == 0) {
                this.interfaces = null;
            } else {
                this.interfaces = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    this.interfaces[i] = pool(toKey(interfaces[i].getName()));
                }
            }
        }

        public boolean hasSuper(String superclass) {
            if (this.superclass == null) return false;
            if (this.superclass.equals(superclass)) return true;
            ClassInfo parentInfo = this.parent;
            if (parentInfo == null) parentInfo = superclasses.get(this.superclass);
            if (parentInfo == null) parentInfo = declareClass(this.superclass);
            if (parentInfo == null) return false;
            this.parent = parentInfo;
            return parentInfo.hasSuper(superclass);
        }

        private static String pool(String s) {
            return STRING_POOL.computeIfAbsent(s, Function.identity());
        }
    }
}
