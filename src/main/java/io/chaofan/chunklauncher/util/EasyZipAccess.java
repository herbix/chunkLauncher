package io.chaofan.chunklauncher.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class EasyZipAccess {

    public static boolean extractZip(String fileName, String tempFolder, String dest, List<String> excludes, String filePrefix) {

        try {
            ZipFile toUnZip = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> list = toUnZip.entries();
            ZipEntry fileInZip;

            while (list.hasMoreElements()) {
                fileInZip = list.nextElement();
                if (fileInZip.isDirectory())
                    continue;
                String name = fileInZip.getName();

                if (excludes != null) {

                    boolean inExclude = false;
                    for (String s : excludes) {
                        if (s.endsWith("/")) {
                            if (name.startsWith(s)) {
                                inExclude = true;
                                break;
                            }
                        } else {
                            if (name.equals(s)) {
                                inExclude = true;
                                break;
                            }
                        }
                    }

                    if (inExclude)
                        continue;
                }

                int lastIndex = name.lastIndexOf('/') + 1;
                String prefixedName = "";

                if (lastIndex != -1) {
                    prefixedName = name.substring(0, lastIndex);
                }

                prefixedName += filePrefix;
                prefixedName += name.substring(lastIndex);

                try {
                    DataInputStream in = new DataInputStream(toUnZip.getInputStream(fileInZip));

                    File extractedFile = new File(tempFolder + prefixedName);
                    extractedFile.getParentFile().mkdirs();
                    DataOutputStream out = new DataOutputStream(new FileOutputStream(extractedFile));

                    int count = 0;
                    byte[] buffer = new byte[4096];
                    while (count >= 0) {
                        count = in.read(buffer);
                        if (count > 0) {
                            out.write(buffer, 0, count);
                        }
                    }

                    out.close();
                    in.close();

                    File extractedFileReal = new File(dest + prefixedName);
                    extractedFileReal.getParentFile().mkdirs();
                    extractedFileReal.delete();
                    Files.move(extractedFile.toPath(), extractedFileReal.toPath());

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean checkHasAll(String fileName, String dest, List<String> excludes, String filePrefix) {

        try {
            ZipFile toUnZip = new ZipFile(fileName);
            Enumeration<? extends ZipEntry> list = toUnZip.entries();
            ZipEntry fileInZip;

            while (list.hasMoreElements()) {
                fileInZip = list.nextElement();
                if (fileInZip.isDirectory())
                    continue;
                String name = fileInZip.getName();

                if (excludes != null) {

                    boolean inExclude = false;
                    for (String s : excludes) {
                        if (s.endsWith("/")) {
                            if (name.startsWith(s)) {
                                inExclude = true;
                                break;
                            }
                        } else {
                            if (name.equals(s)) {
                                inExclude = true;
                                break;
                            }
                        }
                    }

                    if (inExclude)
                        continue;
                }

                int lastIndex = name.lastIndexOf('/') + 1;
                String prefixedName = "";

                if (lastIndex != -1) {
                    prefixedName = name.substring(0, lastIndex);
                }

                prefixedName += filePrefix;
                prefixedName += name.substring(lastIndex);

                File extractedFileReal = new File(dest + prefixedName);

                if (!extractedFileReal.exists()) {
                    return false;
                }
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean generateJar(String fileName, String source, String filePrefix) {
        try {
            JarOutputStream toAdd = new JarOutputStream(new FileOutputStream(fileName));
            source = source.replace('/', System.getProperty("file.separator").charAt(0));
            if (source.endsWith(System.getProperty("file.separator"))) {
                source = source.substring(0, source.length() - 1);
            }
            addAll(toAdd, source, source, filePrefix);
            toAdd.close();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static void addAll(JarOutputStream zip, String filePath, String base, String filePrefix) throws Exception {
        File file = new File(filePath);

        if (file.isDirectory()) {

            File[] list = file.listFiles();

            if (list != null) {
                for (File value : list) {
                    addAll(zip, filePath + System.getProperty("file.separator") + value.getName(), base, filePrefix);
                }
            }

        } else {

            if (!file.getName().startsWith(filePrefix))
                return;

            String name = filePath.substring(base.length() + 1);
            int lastIndex = name.lastIndexOf(System.getProperty("file.separator")) + 1;
            String prefixedName = "";

            if (lastIndex != 0) {
                prefixedName = name.substring(0, lastIndex);
            }

            prefixedName += name.substring(lastIndex + filePrefix.length());

            prefixedName = prefixedName.replace(System.getProperty("file.separator").charAt(0), '/');

            zip.putNextEntry(new JarEntry(prefixedName));

            DataInputStream in = new DataInputStream(new FileInputStream(file));
            int count = 1;
            byte[] buffer = new byte[4096];
            while (count >= 0) {
                count = in.read(buffer);
                if (count > 0) {
                    zip.write(buffer, 0, count);
                }
            }

            in.close();
        }

    }

    public static void addFileListToList(File f, List<String> Innerlist) throws Exception {

        ZipFile file = new ZipFile(f);
        Enumeration<? extends ZipEntry> list = file.entries();
        ZipEntry fileInZip;
        while (list.hasMoreElements()) {
            fileInZip = list.nextElement();
            if (fileInZip.isDirectory())
                continue;
            String name = fileInZip.getName();
            String[] part = name.split("\\.");
            String ext = part[part.length - 1];
            if (ext.equals("class"))
                Innerlist.add(name);
        }
        file.close();
    }
}
