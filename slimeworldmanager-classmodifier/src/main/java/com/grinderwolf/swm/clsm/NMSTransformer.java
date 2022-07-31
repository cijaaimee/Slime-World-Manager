/*
 * Copyright (c) 2022.
 *
 * Author (Fork): Pedro Aguiar
 * Original author: github.com/Grinderwolf/Slime-World-Manager
 *
 * Force, Inc (github.com/rede-force)
 */

package com.grinderwolf.swm.clsm;

import javassist.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSTransformer implements ClassFileTransformer {

    private static final Pattern PATTERN = Pattern.compile("^(\\w+)\\s*\\((.*?)\\)\\s*@(.+?\\.txt)$");
    private static final boolean DEBUG = Boolean.getBoolean("clsmDebug");

    private static Map<String, Change[]> changes = new HashMap<>();

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        instrumentation.addTransformer(new NMSTransformer());

        try (InputStream fileStream = NMSTransformer.class.getResourceAsStream("/list.yml")) {
            if (fileStream == null) {
                System.err.println("Failed to find change list.");
                System.exit(1);

                return;
            }

            Yaml yaml = new Yaml();

            try (InputStreamReader reader = new InputStreamReader(fileStream)) {
                Map<String, Object> data = yaml.load(reader);

                for (String originalClazz : data.keySet()) {
                    boolean optional = originalClazz.startsWith("__optional__");
                    String clazz = originalClazz.substring(optional ? 12 : 0);

                    if (!(data.get(originalClazz) instanceof ArrayList)) {
                        System.err.println("Invalid change list for class " + clazz + ".");

                        continue;
                    }

                    List<String> changeList = (List<String>) data.get(originalClazz);
                    Change[] changeArray = new Change[changeList.size()];

                    for (int i = 0; i < changeList.size(); i++) {
                        Matcher matcher = PATTERN.matcher(changeList.get(i));

                        if (!matcher.find()) {
                            System.err.println("Invalid change '" + changeList.get(i) + "' on class " + clazz + ".");
                            System.exit(1);
                        }

                        String methodName = matcher.group(1);
                        String paramsString = matcher.group(2).trim();
                        String[] parameters;

                        if (paramsString.isEmpty()) {
                            parameters = new String[0];
                        } else {
                            parameters = matcher.group(2).split(",");
                        }

                        String location = matcher.group(3);
                        String content;

                        try (InputStream changeStream = NMSTransformer.class.getResourceAsStream("/" + location)) {
                            if (changeStream == null) {
                                System.err.println("Failed to find data for change "
                                        + changeList.get(i)
                                        + " on class "
                                        + clazz
                                        + ".");
                                System.exit(1);
                            }

                            byte[] contentByteArray = readAllBytes(changeStream);
                            content = new String(contentByteArray, StandardCharsets.UTF_8);
                        }

                        changeArray[i] = new Change(methodName, parameters, content, optional);
                    }

                    if (DEBUG) {
                        System.out.println("Loaded " + changeArray.length + " changes for class " + clazz + ".");
                    }

                    Change[] oldChanges = changes.get(clazz);

                    if (oldChanges == null) {
                        changes.put(clazz, changeArray);
                    } else {
                        Change[] newChanges = new Change[oldChanges.length + changeArray.length];

                        System.arraycopy(oldChanges, 0, newChanges, 0, oldChanges.length);
                        System.arraycopy(changeArray, 0, newChanges, oldChanges.length, changeArray.length);

                        changes.put(clazz, newChanges);
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to load class list.");

            ex.printStackTrace();
        }
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int readLen;

        while ((readLen = stream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, readLen);
        }

        return byteStream.toByteArray();
    }

    @Override
    public byte[] transform(
            ClassLoader classLoader,
            String className,
            Class<?> classBeingTransformed,
            ProtectionDomain protectionDomain,
            byte[] bytes) {
        if (className != null) {
            if (changes.containsKey(className)) {
                String fixedClassName = className.replace("/", ".");

                if (DEBUG) {
                    System.out.println("Applying changes for class " + fixedClassName);
                }

                try {
                    ClassPool pool = ClassPool.getDefault();
                    pool.appendClassPath(new LoaderClassPath(classLoader));
                    CtClass ctClass = pool.get(fixedClassName);

                    for (Change change : changes.get(className)) {
                        CtMethod[] methods = ctClass.getDeclaredMethods(change.getMethodName());
                        boolean found = false;

                        main:
                        for (CtMethod method : methods) {
                            CtClass[] params = method.getParameterTypes();

                            if (params.length != change.getParams().length) {
                                continue;
                            }

                            for (int i = 0; i < params.length; i++) {
                                if (!change.getParams()[i].trim().equals(params[i].getName())) {
                                    continue main;
                                }
                            }

                            found = true;

                            try {
                                method.insertBefore(change.getContent());
                            } catch (CannotCompileException ex) {
                                if (!change.isOptional()) { // If it's an optional change we can ignore
                                    // it
                                    throw ex;
                                }
                            }

                            break;
                        }

                        if (!found && !change.isOptional()) {
                            throw new NotFoundException("Unknown method " + change.getMethodName());
                        }
                    }

                    return ctClass.toBytecode();
                } catch (NotFoundException | CannotCompileException | IOException ex) {
                    System.err.println("Failed to override methods from class " + fixedClassName + ".");
                    ex.printStackTrace();
                }
            }
        }

        return null;
    }

    @Getter
    @RequiredArgsConstructor
    private static class Change {

        private final String methodName;
        private final String[] params;
        private final String content;
        private final boolean optional;
    }
}
