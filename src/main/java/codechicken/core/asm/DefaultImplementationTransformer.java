package codechicken.core.asm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.asm.ASMHelper;
import codechicken.lib.asm.ObfMapping;
import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class DefaultImplementationTransformer implements IClassTransformer {

    private static final HashMap<String, InterfaceImpl> impls = new HashMap<>();

    public static void registerDefaultImpl(String iname, String cname) {
        if (impls.isEmpty()) {
            String name = DefaultImplementationTransformer.class.getName();
            FMLRelaunchLog.finer("Registering transformer %s", name);
            Launch.classLoader.registerTransformer(name);
        }
        impls.put(iname.replace('.', '/'), new InterfaceImpl(iname, cname));
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (transformedName.startsWith("net.minecraft") || impls.isEmpty()) return bytes;

        ClassNode cnode = ASMHelper.createClassNode(bytes);
        boolean changed = false;
        for (String iname : cnode.interfaces) {
            InterfaceImpl impl = impls.get(iname);
            if (impl != null) changed |= impl.patch(cnode);
        }

        return changed ? ASMHelper.createBytes(cnode, 0) : bytes;
    }

    static class InterfaceImpl {

        public final String iname;
        public ArrayList<MethodNode> impls = new ArrayList<>();

        public InterfaceImpl(String iname, String cname) {
            this.iname = iname;
            HashSet<String> names = new HashSet<>();
            ClassNode inode = getClassNode(iname);
            for (MethodNode method : inode.methods) names.add(method.name + method.desc);

            ClassNode cnode = getClassNode(cname);
            for (MethodNode method : cnode.methods) if (names.contains(method.name + method.desc)) {
                impls.add(method);
                method.desc = new ObfMapping(cnode.name, method.name, method.desc).toRuntime().s_desc;
            }
        }

        public boolean patch(ClassNode cnode) {
            LinkedList<String> names = new LinkedList<>();
            for (MethodNode method : cnode.methods) {
                ObfMapping m = new ObfMapping(cnode.name, method.name, method.desc).toRuntime();
                names.add(m.s_name + m.s_desc);
            }

            boolean changed = false;
            for (MethodNode impl : impls) {
                if (names.contains(impl.name + impl.desc)) continue;

                MethodNode copy = new MethodNode(
                        impl.access,
                        impl.name,
                        impl.desc,
                        impl.signature,
                        impl.exceptions == null ? null : impl.exceptions.toArray(new String[0]));
                ASMHelper.copy(impl, copy);
                cnode.methods.add(impl);
                changed = true;
            }
            return changed;
        }

        private static ClassNode getClassNode(String name) {
            try {
                return ASMHelper.createClassNode(Launch.classLoader.getClassBytes(name.replace('/', '.')));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
