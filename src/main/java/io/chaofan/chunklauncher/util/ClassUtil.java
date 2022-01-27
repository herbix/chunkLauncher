package io.chaofan.chunklauncher.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtil {

    public static Class<?>[] getClassesFromPackage(String packageName, boolean searchJre) {

        String classPathString = System.getProperty("java.class.path");

        if (searchJre) {
            classPathString += System.getProperty("path.separator") + System.getProperty("sun.boot.class.path");
        }

        String[] classPaths = classPathString.split(System.getProperty("path.separator"));

        List<Class<?>> results = new ArrayList<>();

        String packagePath = packageName.replaceAll("\\.", "/");

        for (String classPath : classPaths) {
            getClassesFromPackage(packageName, results, packagePath, classPath, ClassUtil.class.getClassLoader());
        }

        return results.toArray(new Class<?>[0]);
    }

    public static boolean isInClassPath(String path) {
        String classPathString = System.getProperty("java.class.path");
        classPathString += System.getProperty("path.separator") + System.getProperty("sun.boot.class.path");

        String[] classPaths = classPathString.split(System.getProperty("path.separator"));
        File pathFile = new File(path).getAbsoluteFile();
        for (String classPath : classPaths) {
            if (new File(classPath).getAbsoluteFile().equals(pathFile)) {
                return true;
            }
        }

        return false;
    }

    public static String[] listResources(String resourcesPath) {
        String classPathString = System.getProperty("java.class.path");

        String[] classPaths = classPathString.split(System.getProperty("path.separator"));

        List<String> results = new ArrayList<>();

        for (String classPath : classPaths) {
            results.addAll(getItemsFromPackage(resourcesPath, classPath));
        }

        return results.toArray(new String[0]);
    }

    public static Class<?>[] getClassesFromPackage(String packageName, String classPath, boolean useNewLoader) {
        List<Class<?>> results = new ArrayList<>();
        String packagePath = packageName.replaceAll("\\.", "/");

        ClassLoader loader = ClassUtil.class.getClassLoader();
        if (useNewLoader) {
            try {
                loader = new URLClassLoader(new URL[]{new File(classPath).toURI().toURL()}, loader);
            } catch (MalformedURLException e) {
                return new Class<?>[0];
            }
        }

        getClassesFromPackage(packageName, results, packagePath, classPath, loader);
        return results.toArray(new Class<?>[0]);
    }

    private static void getClassesFromPackage(String packageName, List<Class<?>> results, String packagePath, String classPath, ClassLoader loader) {
        for (String file : getItemsFromPackage(packagePath, classPath)) {
            if (file.endsWith(".class") && !file.contains("$")) {
                try {
                    results.add(loader.loadClass(packageName + "." + file.substring(0, file.length() - 6)));
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }

    private static List<String> getItemsFromPackage(String packagePath, String classPath) {
        List<String> result = new ArrayList<>();
        File cpFile = new File(classPath);

        if (cpFile.isDirectory()) {
            File pFile = new File(cpFile, packagePath);

            if (pFile.isDirectory()) {
                File[] files = pFile.listFiles();
                if (files == null) {
                    return result;
                }
                for (File cFile : files) {
                    String cFileName = cFile.getName();
                    if (cFile.isFile()) {
                        result.add(cFileName);
                    }
                }
            }
        } else if (cpFile.isFile()) {
            try {
                JarFile jFile = new JarFile(cpFile);

                Enumeration<JarEntry> jEntrys = jFile.entries();

                while (jEntrys.hasMoreElements()) {
                    JarEntry jClass = jEntrys.nextElement();
                    String jClassPath = jClass.getName();

                    if (jClassPath.startsWith(packagePath)) {
                        String jClassName = jClassPath.substring(packagePath.length() + 1);
                        if (!jClassName.contains("/")) {
                            result.add(jClassName);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return result;
    }
}
