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

        if(searchJre) {
            classPathString += System.getProperty("path.separator") + System.getProperty("sun.boot.class.path");
        }

        String[] classPaths = classPathString.split(System.getProperty("path.separator"));

        List<Class<?>> results = new ArrayList<Class<?>>();

        String packagePath = packageName.replaceAll("\\.", "/");

        for(String classPath : classPaths) {
            getClassesFromPackage(packageName, results, packagePath, classPath, ClassUtil.class.getClassLoader());
        }

        return results.toArray(new Class<?>[results.size()]);
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

    public static Class<?>[] getClassesFromPackage(String packageName, String classPath, boolean useNewLoader) {
        List<Class<?>> results = new ArrayList<Class<?>>();
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
        return results.toArray(new Class<?>[results.size()]);
    }

    private static void getClassesFromPackage(String packageName, List<Class<?>> results, String packagePath, String classPath, ClassLoader loader) {
        File cpFile = new File(classPath);

        if(cpFile.isDirectory()) {
            File pFile = new File(cpFile, packagePath);

            if(pFile.isDirectory()) {
                File[] files = pFile.listFiles();
                if (files == null) {
                    return;
                }
                for(File cFile : files) {
                    String cFileName = cFile.getName();

                    if(cFile.isFile() && cFileName.endsWith(".class") && !cFileName.contains("$")) {
                        try {
                            results.add(loader.loadClass(packageName + "." + cFileName.substring(0, cFileName.length() - 6)));
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
            }
        } else if(cpFile.isFile()) {
            try {
                JarFile jFile = new JarFile(cpFile);

                Enumeration<JarEntry> jEntrys = jFile.entries();

                while(jEntrys.hasMoreElements()) {
                    JarEntry jClass = jEntrys.nextElement();
                    String jClassPath = jClass.getName();

                    if(jClassPath.startsWith(packagePath)) {
                        String jClassName = jClassPath.substring(packagePath.length() + 1);
                        if(!jClassName.contains("/") && jClassName.endsWith(".class") && !jClassName.contains("$")) {
                            try {
                                results.add(loader.loadClass(packageName + "." + jClassName.substring(0, jClassName.length() - 6)));
                            } catch (ClassNotFoundException ignored) {
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
