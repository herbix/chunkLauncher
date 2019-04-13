package io.chaofan.chunklauncher.version;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import io.chaofan.chunklauncher.Config;

public class ModuleManager {

    public static Module[] modules = null;

    private static Map<Integer, Module> moduleFromListItem = new HashMap<Integer, Module>();
    private static Map<String, Module> moduleFromChoiceItem = new HashMap<String, Module>();
    private static Map<String, Module> moduleFromName = new HashMap<String, Module>();

    public synchronized static void showModules(DefaultTableModel model) {
        model.setRowCount(0);

        moduleFromListItem.clear();
        for(int i=0, j=0; i<modules.length; i++) {
            Module m = modules[i];
            if(!m.isInstalled() && !Config.showOld && m.getType().startsWith("old")) {
                continue;
            }
            if(!m.isInstalled() && !Config.showSnapshot && m.getType().startsWith("snapshot")) {
                continue;
            }
            model.addRow(new String[]{ m.getState(), m.getName(), m.getType(), m.getFormattedReleaseTime() });
            moduleFromListItem.put(j, m);
            j++;
        }
    }

    public synchronized static void showModules(JComboBox<String> list) {
        list.removeAllItems();
        moduleFromChoiceItem.clear();
        for(int i=0; i<modules.length; i++) {
            Module m = modules[i];
            if(m.isInstalled()) {
                list.addItem(m.getName());
                moduleFromChoiceItem.put(m.getName(), m);
            }
        }
    }

    public static Module getSelectedModule(JTable table) {
        return moduleFromListItem.get(table.getSelectedRow());
    }

    public static RunnableModule getSelectedModule(JComboBox<String> list) {
        return (RunnableModule) moduleFromChoiceItem.get(list.getSelectedItem());
    }

    public synchronized static void initModules(Map<String, Version> versions,
        ModuleInstallCallback icallback, ModuleUninstallCallback ucallback) {

        List<Module> moduleList = new ArrayList<Module>();

        Set<String> versionIds = versions.keySet();
        for(String key : versionIds) {
            Version elem = versions.get(key);
            RunnableModule rm = new RunnableModule(icallback, ucallback);
            rm.version = elem;
            if(moduleFromName.containsKey(rm.getName()))
                moduleList.add(moduleFromName.get(rm.getName()));
            else
                moduleList.add(rm);
        }

        File versionFolder = new File(Config.gamePath + "/versions");
        if(versionFolder.isDirectory()) {
            for(File inFolder : versionFolder.listFiles()) {
                if(inFolder.isDirectory() && !versionIds.contains(inFolder.getName())) {

                    File jsonFile = new File(inFolder, inFolder.getName() + ".json");

                    if(jsonFile.isFile()) {
                        Version ver = new Version();
                        ver.id = inFolder.getName();
                        RunnableModule rm = new RunnableModule(icallback, ucallback);
                        rm.version = ver;
                        if(moduleFromName.containsKey(rm.getName()))
                            moduleList.add(moduleFromName.get(rm.getName()));
                        else
                            moduleList.add(rm);
                    }

                }
            }
        }

        Collections.sort(moduleList, new Comparator<Module>(){
            public int compare(Module o1, Module o2) {
                return -o1.getActualReleaseTime().compareTo(o2.getActualReleaseTime());
            }
        });

        modules = moduleList.toArray(new Module[moduleList.size()]);

        moduleFromName.clear();
        for(Module m : modules) {
            moduleFromName.put(m.getName(), m);
        }

        for(Module m : modules) {
            m.getState();
        }
    }

    public static Module getModuleFromName(String name) {
        return moduleFromName.get(name);
    }
}
