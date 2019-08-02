package com.grinderwolf.smw.crlfixer;

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

public class PlayerChunkMapModifier implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new PlayerChunkMapModifier());
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingTransformed, ProtectionDomain protectionDomain, byte[] bytes) {
        if (className != null) {
            String fixedClassName = className.replace("/", ".");

            try {
                if (fixedClassName.endsWith("PlayerChunkMap")) {
                    ClassPool pool = ClassPool.getDefault();
                    pool.appendClassPath(new ByteArrayClassPath(fixedClassName, bytes));
                    CtClass ctClass = pool.get(fixedClassName);

                    // Load chunk
                    CtMethod[] loadChunkMethods = ctClass.getDeclaredMethods("f");

                    for (CtMethod method : loadChunkMethods) {
                        if (method.getParameterTypes().length == 0) {
                            // There is another f() method which we're not interested in
                            continue;
                        }

                        method.insertBefore("{ " +
                                "    java.util.concurrent.CompletableFuture chunk = com.grinderwolf.smw.crlfixer.CRLFixer.getFutureChunk($0.world, $1.x, $1.z);" +
                                "    if (chunk != null) { " +
                                "        return chunk;" +
                                "    }" +
                                "}");
                    }

                    // Save chunk
                    CtMethod saveChunkMethod = ctClass.getDeclaredMethod("saveChunk");

                    saveChunkMethod.insertBefore("{" +
                            "    if (com.grinderwolf.smw.crlfixer.CRLFixer.saveChunk($0.world, $1)) {" +
                            "        return false;" +
                            "    }" +
                            "}");

                    return ctClass.toBytecode();
                }
            } catch (NotFoundException ex) {
                System.err.println("Failed to locate chunk load and save methods from class " + fixedClassName + ". Is this server running spigot 1.14?");

                ex.printStackTrace();
            } catch (CannotCompileException | IOException ex) {
                System.err.println("Failed to update chunk load and save methods from class " + fixedClassName + ".");
                ex.printStackTrace();
            }
        }

        return null;
    }
}
