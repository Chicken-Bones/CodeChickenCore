package codechicken.lib.asm;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import codechicken.core.launch.CodeChickenCorePlugin;

public class RedirectorTransformer implements IClassTransformer, Opcodes {

    private static final boolean DUMP_CLASSES = Boolean.parseBoolean(System.getProperty("ccl.dumpClass", "false"));
    private static final String RenderStateClass = "codechicken/lib/render/CCRenderState";
    private static final Set<String> redirectedFields = new HashSet<>();
    private static final Set<String> redirectedSimpleMethods = new HashSet<>();
    private static final Set<String> redirectedMethods = new HashSet<>();
    private static final ClassConstantPoolParser cstPoolParser;

    static {
        Collections.addAll(
                redirectedFields,
                "pipeline",
                "model",
                "firstVertexIndex",
                "lastVertexIndex",
                "vertexIndex",
                "baseColour",
                "alphaOverride",
                "useNormals",
                "computeLighting",
                "useColour",
                "lightMatrix",
                "vert",
                "hasNormal",
                "normal",
                "hasColour",
                "colour",
                "hasBrightness",
                "brightness",
                "side",
                "lc",
                "normalAttrib",
                "colourAttrib",
                "lightCoordAttrib",
                "sideAttrib",
                "lightingAttrib"

        );
        Collections.addAll(redirectedSimpleMethods, "reset", "pullLightmap", "pushLightmap", "setDynamic", "draw");
        Collections.addAll(
                redirectedMethods,
                "setPipeline",
                "bindModel",
                "setModel",
                "setVertexRange",
                "render",
                "runPipeline",
                "writeVert",
                "setNormal",
                "setColour",
                "setBrightness",
                "startDrawing");

        cstPoolParser = new ClassConstantPoolParser(RenderStateClass);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!cstPoolParser.find(basicClass)) {
            return basicClass;
        }

        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        boolean changed = false;

        // spotless:off
        for (MethodNode mn : cn.methods) {
            for (AbstractInsnNode node : mn.instructions.toArray()) {
                if (node instanceof FieldInsnNode fNode) {
                    if (node.getOpcode() == GETSTATIC && RenderStateClass.equals(fNode.owner) && redirectedFields.contains(fNode.name)) {
                        mn.instructions.insertBefore(fNode,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                fNode.owner,
                                "instance",
                                "()Lcodechicken/lib/render/CCRenderState;",
                                false));
                        fNode.setOpcode(GETFIELD);
                        changed = true;
                    } else if (node.getOpcode() == PUTSTATIC && RenderStateClass.equals(fNode.owner) && redirectedFields.contains(fNode.name)) {
                        InsnList list = new InsnList();
                        list.add(new MethodInsnNode(
                            INVOKESTATIC,
                            fNode.owner,
                            "instance",
                            "()Lcodechicken/lib/render/CCRenderState;",
                            false));
                        list.add(new InsnNode(SWAP));
                        mn.instructions.insertBefore(fNode, list);
                        fNode.setOpcode(PUTFIELD);
                        changed = true;
                    }
                } else if (node instanceof MethodInsnNode mNode) {
                    if (node.getOpcode() == INVOKESTATIC && RenderStateClass.equals(mNode.owner) && redirectedSimpleMethods.contains(mNode.name)) {
                        mn.instructions.insertBefore(mNode,
                            new MethodInsnNode(
                                INVOKESTATIC,
                                mNode.owner,
                                "instance",
                                "()Lcodechicken/lib/render/CCRenderState;",
                                false));
                        mNode.setOpcode(INVOKEVIRTUAL);
                        mNode.name = mNode.name + "Instance";
                        changed = true;
                    } else if (node.getOpcode() == INVOKEVIRTUAL && RenderStateClass.equals(mNode.owner)
                        && (redirectedSimpleMethods.contains(mNode.name) || redirectedMethods.contains(mNode.name))) {
                        // Handle mods that updated to previously new API
                        mNode.name = mNode.name + "Instance";
                        changed = true;
                    }
                }
            }
        }
        // spotless:on

        if (changed) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);
            final byte[] bytes = cw.toByteArray();
            if (DUMP_CLASSES) {
                saveTransformedClass(basicClass, transformedName + "_PRE");
                saveTransformedClass(bytes, transformedName + "_POST");
            }
            return bytes;
        }
        return basicClass;
    }

    private File outputDir = null;

    private void saveTransformedClass(final byte[] data, final String classname) {
        if (outputDir == null) {
            outputDir = new File(Launch.minecraftHome, "ASM_CCL" + File.separatorChar + "REDIRECTOR");
            try {
                FileUtils.deleteDirectory(outputDir);
            } catch (IOException ignored) {}
            if (!outputDir.exists()) {
                // noinspection ResultOfMethodCallIgnored
                outputDir.mkdirs();
            }
        }
        final String fileName = classname.replace('.', File.separatorChar);
        final File classFile = new File(outputDir, fileName + ".class");
        final File bytecodeFile = new File(outputDir, fileName + "_BYTE.txt");
        final File outDir = classFile.getParentFile();
        if (!outDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            outDir.mkdirs();
        }
        if (classFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            classFile.delete();
        }
        try (final OutputStream output = Files.newOutputStream(classFile.toPath())) {
            output.write(data);
            CodeChickenCorePlugin.logger.info("Saved class (byte[]) to " + classFile.toPath());
        } catch (IOException e) {
            CodeChickenCorePlugin.logger.error("Could not save class (byte[]) " + classname);
        }
        if (bytecodeFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            bytecodeFile.delete();
        }
        try (final OutputStream output = Files.newOutputStream(bytecodeFile.toPath())) {
            final ClassReader classReader = new ClassReader(data);
            classReader.accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(output)), 0);
            CodeChickenCorePlugin.logger.info("Saved class (bytecode) to " + bytecodeFile.toPath());
        } catch (IOException e) {
            CodeChickenCorePlugin.logger.error("Could not save class (bytecode) " + classname);
        }
    }
}
