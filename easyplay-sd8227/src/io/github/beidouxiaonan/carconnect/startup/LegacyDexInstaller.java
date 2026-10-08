package io.github.beidouxiaonan.carconnect.startup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Runtime-signature adapter; MultiDex still owns extraction, locking and CRC checks. */
public final class LegacyDexInstaller {
    public interface Reporter { void stage(String message); }
    private static Reporter reporter;
    private LegacyDexInstaller() {}
    public static void setReporter(Reporter value) { reporter = value; }
    private static void log(String message) {
        if (reporter != null) try { reporter.stage(message); } catch (RuntimeException ignored) {}
    }
    private static Field field(Object object, String name) throws NoSuchFieldException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(object.getClass().getName() + "." + name);
    }
    private static boolean listType(Class<?> type) {
        return type == ArrayList.class || type == List.class;
    }
    private static int shape(Method method) {
        if (!method.getName().equals("makeDexElements") || !method.getReturnType().isArray()
                || method.getReturnType().getComponentType().isPrimitive()) return 0;
        Class<?>[] p = method.getParameterTypes();
        if (p.length < 2 || !listType(p[0]) || p[1] != File.class) return 0;
        if (p.length == 2) return 2;
        if (p.length == 3 && listType(p[2])) return 3;
        if (p.length == 4 && listType(p[2]) && p[3] == ClassLoader.class) return 4;
        return 0;
    }
    private static String signature(Method method) {
        StringBuilder text = new StringBuilder(method.getDeclaringClass().getName());
        text.append('.').append(method.getName()).append('(');
        Class<?>[] p = method.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) text.append(','); text.append(p[i].getName());
        }
        return text.append(')').toString();
    }
    private static Method factory(Object pathList) throws NoSuchMethodException {
        ArrayList<Method> candidates = new ArrayList<Method>();
        ArrayList<String> available = new ArrayList<String>();
        for (Class<?> type = pathList.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("makeDexElements") || method.getName().equals("makePathElements"))
                    available.add(signature(method));
                if (shape(method) != 0) candidates.add(method);
            }
        }
        Collections.sort(available);
        log("DEX_FACTORY_AVAILABLE " + available);
        // Prefer the error-reporting three-argument API, then the old two-argument API.
        // Select once; invocation errors must not trigger a different, potentially unsafe factory.
        for (int arity : new int[] {3, 2, 4}) {
            Method chosen = null;
            for (Method method : candidates) if (shape(method) == arity) {
                if (chosen == null || signature(method).compareTo(signature(chosen)) < 0) chosen = method;
            }
            if (chosen != null) { chosen.setAccessible(true); return chosen; }
        }
        throw new NoSuchMethodException("No supported makeDexElements signature: " + available);
    }
    public static synchronized void install(ClassLoader loader, List<? extends File> files, File directory)
            throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, IOException {
        if (files == null) throw new IllegalArgumentException("Missing secondary dex list");
        if (files.isEmpty()) return;
        for (File file : files) if (file == null || !file.isFile() || !file.canRead())
            throw new IOException("Secondary dex file unavailable");
        Object pathList = field(loader, "pathList").get(loader);
        if (pathList == null) throw new IllegalStateException("Missing pathList");
        Field elements;
        try { elements = field(pathList, "dexElements"); }
        catch (NoSuchFieldException missing) { elements = field(pathList, "pathElements"); }
        Object old = elements.get(pathList);
        if (!(old instanceof Object[])) throw new IllegalStateException("Invalid existing dex elements");
        Class<?> component = old.getClass().getComponentType();
        Method make = factory(pathList);
        log("DEX_FACTORY_SELECTED " + signature(make) + " files=" + files.size());
        ArrayList<File> input = new ArrayList<File>(files);
        ArrayList<IOException> errors = new ArrayList<IOException>();
        Object[] args = shape(make) == 2 ? new Object[] {input, directory}
            : shape(make) == 3 ? new Object[] {input, directory, errors}
            : new Object[] {input, directory, errors, loader};
        Object created;
        log("DEX_CREATE_BEGIN");
        try { created = make.invoke(pathList, args); }
        catch (InvocationTargetException error) {
            if (error.getCause() instanceof IOException) throw (IOException) error.getCause();
            throw error;
        }
        // Keep the live classpath untouched until all secondary files load successfully.
        if (!errors.isEmpty()) {
            IOException error = new IOException("DEX factory reported " + errors.size() + " load error(s)");
            error.initCause(errors.get(0)); throw error;
        }
        if (!(created instanceof Object[])) throw new IOException("DEX factory returned no elements");
        Object[] added = (Object[]) created;
        if (added.length != input.size()) throw new IOException("Incomplete secondary DEX: expected "
            + input.size() + " elements, got " + added.length);
        for (Object item : added) {
            if (item == null || !component.isInstance(item)) throw new IOException("Invalid secondary DEX element type");
            // Old two-argument factories can hide IOException and return a ZIP-only element.
            // Every extracted secondary archive must contain an actual loaded DexFile.
            if (field(item, "dexFile").get(item) == null) throw new IOException("Secondary DEX did not load");
        }
        if (elements.get(pathList) != old) throw new IllegalStateException("DEX factory changed live classpath");
        log("DEX_CREATE_OK count=" + added.length);
        Object[] before = (Object[]) old;
        Object[] combined = (Object[]) Array.newInstance(component, before.length + added.length);
        System.arraycopy(before, 0, combined, 0, before.length);
        System.arraycopy(added, 0, combined, before.length, added.length);
        log("DEX_APPEND_BEGIN old=" + before.length + " new=" + added.length);
        elements.set(pathList, combined);
        log("DEX_APPEND_OK total=" + combined.length);
    }
}
