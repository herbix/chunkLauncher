package io.chaofan.chunklauncher.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class EasyFileAccess {

    public static String loadFile(String path) {
        try (InputStream in = new FileInputStream(path)) {
            return loadInputStream(in);
        } catch (Exception e) {
            return null;
        }
    }

    public static String loadInputStream(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            int n;
            byte[] buffer = new byte[65536];
            while ((n = in.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            return null;
        }

        return new String(out.toByteArray());
    }

    public static boolean saveFile(String path, String content) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            out.write(content);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean deleteFileForce(File file) {
        boolean result = true;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!deleteFileForce(f))
                        result = false;
                }
            }
        }
        if (file.exists())
            result &= file.delete();
        return result;
    }

    public static boolean copyDirectory(File file, File targetFile) {
        if (file.isDirectory()) {
            boolean result = true;
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    result &= copyDirectory(f, new File(targetFile, f.getName()));
                }
            }
            return result;
        } else if (file.isFile()) {
            return copyFile(file, targetFile);
        } else {
            return false;
        }
    }

    public static boolean copyFile(File file, File targetFile) {
        try {
            FileInputStream in = new FileInputStream(file);
            targetFile.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(targetFile);

            int len;
            byte[] buffer = new byte[65536];
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getDigest(File file, String algorithm, int hashLength) {
        try (DigestInputStream stream = new DigestInputStream(new FileInputStream(file),
                MessageDigest.getInstance(algorithm))) {
            byte[] buffer = new byte[65536];
            int read;
            do {
                read = stream.read(buffer);
            } while (read > 0);

            return String.format("%1$0" + hashLength + "x", new BigInteger(1, stream.getMessageDigest().digest()));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean doSha1Checksum(String shaFilePath, String checkedFilePath) {
        File checkedFile = new File(checkedFilePath);
        if (!checkedFile.isFile()) {
            return false;
        }
        String checksum = loadFile(shaFilePath);
        if (checksum == null) {
            return true;
        }
        String checksum2 = getDigest(checkedFile, "SHA-1", 40);
        return checksum.equalsIgnoreCase(checksum2);
    }

    public static boolean doSha1Checksum2(String checksum, String checkedFilePath) {
        File checkedFile = new File(checkedFilePath);
        if (!checkedFile.isFile()) {
            return false;
        }
        if (checksum == null) {
            return true;
        }
        String checksum2 = getDigest(checkedFile, "SHA-1", 40);
        return checksum.equalsIgnoreCase(checksum2);
    }
}
