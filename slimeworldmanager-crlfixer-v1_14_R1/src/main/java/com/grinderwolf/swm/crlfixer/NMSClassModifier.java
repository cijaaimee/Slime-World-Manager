package com.grinderwolf.swm.crlfixer;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class NMSClassModifier implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new NMSClassModifier());
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingTransformed, ProtectionDomain protectionDomain, byte[] bytes) {
        if (className != null) {
            String fixedClassName = className.replace("/", ".");

            try {
                if (fixedClassName.endsWith("PlayerChunkMap")) {
                    return fixPlayerChunkMap(fixedClassName, bytes).toBytecode();
                }
            } catch (NotFoundException ex) {
                System.err.println("Failed to override NMS methods from class " + fixedClassName + ". Is this server running spigot 1.14?");

                ex.printStackTrace();
            } catch (CannotCompileException | IOException ex) {
                System.err.println("Failed to override NMS methods from class " + fixedClassName + ". ");
                ex.printStackTrace();
            }
        }

        return null;
    }

    private CtClass fixPlayerChunkMap(String className, byte[] clazz) throws NotFoundException, CannotCompileException {
        ClassPool pool = ClassPool.getDefault();
        pool.appendClassPath(new ByteArrayClassPath(className, clazz));
        CtClass ctClass = pool.get(className);

        // Load chunk
        CtMethod[] loadChunkMethods = ctClass.getDeclaredMethods("f");

        for (CtMethod method : loadChunkMethods) {
            if (method.getParameterTypes().length == 0) {
                // There is another f() method which we're not interested in
                continue;
            }

            method.insertBefore("{ " +
                    "    java.util.concurrent.CompletableFuture chunk = com.grinderwolf.swm.crlfixer.CRLFixer.getFutureChunk($0.world, $1.x, $1.z);" +
                    "    if (chunk != null) { " +
                    "        return chunk;" +
                    "    }" +
                    "}");
        }

        // Save chunk
        CtMethod saveChunkMethod = ctClass.getDeclaredMethod("saveChunk");

        saveChunkMethod.insertBefore("{" +
                "    if (com.grinderwolf.swm.crlfixer.CRLFixer.saveChunk($0.world, $1)) {" +
                "        return false;" +
                "    }" +
                "}");

        return ctClass;
    }
}
