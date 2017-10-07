package io.chaofan.chunklauncher;

/**
 * Created by Chaofan on 2017/10/7.
 */
public class RunningDirectory {
    public String name;
    public String directory;

    public RunningDirectory(String name, String directory) {
        this.name = name;
        this.directory = directory;
    }

    @Override
    public String toString() {
        return name;
    }
}
