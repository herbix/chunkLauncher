package io.chaofan.chunklauncher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class UpdaterLater {

    public static void main(String[] args) {
/*
        try {
            PrintStream out = new PrintStream(new FileOutputStream("log"));
            System.setErr(out);
            System.setOut(out);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
    */

        if (args.length <= 0) {
            return;
        }

        File tempFile = new File(args[0]);
        File realFile = new File(args[1]);

        if (!tempFile.isFile())
            return;

        boolean deleted = false;

        if (realFile.isFile())
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (realFile.delete()) {
                    deleted = true;
                    break;
                }
            }

        if (!deleted) {
            return;
        }

        for (int i = 0; i < 10; i++) {
            try {
                Files.move(tempFile.toPath(), realFile.toPath());
                break;
            } catch (IOException ignored) {
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        realFile.setExecutable(true);

        String java = System.getProperty("java.home") + "/bin/java";

        if (getCurrentPlatform().equals("windows")) {
            if (new File(java + "w.exe").exists()) {
                java += "w.exe";
            }
        }

        try {
            new ProcessBuilder(java, "-jar", realFile.getAbsolutePath()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getCurrentPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win"))
            return "windows";

        return "other";
    }

}
