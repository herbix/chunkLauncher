package io.chaofan.chunklauncher;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.auth.AuthType;
import io.chaofan.chunklauncher.download.Downloader;
import io.chaofan.chunklauncher.util.Lang;
import io.chaofan.chunklauncher.version.Version;

public class LauncherFrame extends JFrame {

    private static final long serialVersionUID = -1923022699772187651L;

    JPanel base = new JPanel();

    JLabel modulesLabel = new JLabel(Lang.getString("ui.module.label"));
    DefaultTableModel modulesModel = new UneditableTableModel();
    JTable modules = new JTable(modulesModel);
    JScrollPane modulesOuter = new JScrollPane(modules);
    JButton installModules = new JButton(Lang.getString("ui.module.install"));
    JButton uninstallModules = new JButton(Lang.getString("ui.module.uninstall"));
    JCheckBox showOld = new JCheckBox(Lang.getString("ui.module.old"));
    JCheckBox showSnapshot = new JCheckBox(Lang.getString("ui.module.snapshot"));

    JLabel userLabel = new JLabel(Lang.getString("ui.username.label"));
    JTextField user = new JTextField();
    JLabel passLabel = new JLabel(Lang.getString("ui.password.label"));
    JTextField pass = new JPasswordField();
    JCheckBox savePass = new JCheckBox(Lang.getString("ui.savepassword"));
    JLabel authTypeLabel = new JLabel(Lang.getString("ui.auth.type.label"));
    JComboBox<AuthType> authType = new JComboBox<AuthType>();
    JLabel gameVersionLabel = new JLabel(Lang.getString("ui.version.label"));
    JComboBox<String> gameVersion = new JComboBox<String>();
    JButton launch = new JButton(Lang.getString("ui.launch"));

    JLabel profilesLabel = new JLabel(Lang.getString("ui.profile.label"));
    JComboBox<Profile> profiles = new JComboBox<Profile>();
    JLabel profileDetailLabel = new JLabel();

    JButton addProfile = new JButton(Lang.getString("ui.profile.add"));
    JButton removeProfile = new JButton(Lang.getString("ui.profile.remove"));

    JLabel runPathLabel = new JLabel(Lang.getString("ui.runpath.label"));
    JTextField runPath = new JTextField();
    JButton runPathSearch = new JButton("...");

    ButtonGroup runningMode = new ButtonGroup();
    JRadioButton runningMode32 = new JRadioButton(Lang.getString("ui.mode.d32"), false);
    JRadioButton runningMode64 = new JRadioButton(Lang.getString("ui.mode.d64"), false);
    JRadioButton runningModeDefault = new JRadioButton(Lang.getString("ui.mode.default"), true);
    JLabel jrePathLabel = new JLabel(Lang.getString("ui.jrepath.label"));
    JTextField jrePath = new JTextField();
    JButton jrePathSearch = new JButton("...");
    JLabel memorySizeLabel = new JLabel(Lang.getString("ui.memory.label"));
    JTextField memorySize = new JTextField();
    JSlider memorySizeSlider = new JSlider();

    JLabel commentLabel = new JLabel();
    JProgressBar progress = new JProgressBar();

    JLabel proxyTypeLabel = new JLabel(Lang.getString("ui.proxy.type.label"));
    JLabel proxyHostPortLabel = new JLabel(Lang.getString("ui.proxy.hostport.label"));
    JCheckBox enableProxy = new JCheckBox(Lang.getString("ui.proxy.enable.label"));
    JComboBox<String> proxyType = new JComboBox<String>(new String[]{"HTTP", "Socks"});
    JTextField proxy = new JTextField();

    JButton profileSetting = new JButton(Lang.getString("ui.tab.profile"));
    JButton moduleSetting = new JButton(Lang.getString("ui.tab.module"));
    JButton systemSetting = new JButton(Lang.getString("ui.tab.system"));
    JPanel profilePanel = new JPanel();
    JPanel modulePanel = new JPanel();
    JPanel systemPanel = new JPanel();

    PrintStream thisStdOut = null;
    PrintStream oldStdOut = null;

    private PrintStream oldStdErr;

    public LauncherFrame() {
        addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e) {
                if(thisStdOut != null && thisStdOut == System.out) {
                    System.setOut(oldStdOut);
                }
                Config.updateFromFrame(LauncherFrame.this);
                Config.saveConfig();
                Downloader.stopAll();
                dispose();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });

        setResizable(false);
        setTitle("ChunkLauncher for Minecraft V" + Launcher.VERSION + " (Made by Chaofan)");
        setIconImage(new ImageIcon(getClass().getResource("/favicon.png")).getImage());
        base.setPreferredSize(new Dimension(600, 400));
        add(base);
        pack();
        setLocationByPlatform(true);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);

        base.setLayout(null);
        createFrame();
        base.getRootPane().setDefaultButton(launch);

        this.setFocusTraversalPolicy(new LauncherFrameFocusTraversalPolicy());
    }

    private void createFrame() {

        base.add(commentLabel);
        commentLabel.setLocation(0, 360);
        commentLabel.setSize(600, 20);

        base.add(progress);
        progress.setLocation(0, 380);
        progress.setSize(600, 20);
        HttpFetcher.setJProgressBar(progress);

        base.add(profileSetting);
        profileSetting.setLocation(440, 10);
        profileSetting.setSize(150, 40);

        base.add(moduleSetting);
        moduleSetting.setLocation(440, 60);
        moduleSetting.setSize(150, 40);

        base.add(systemSetting);
        systemSetting.setLocation(440, 110);
        systemSetting.setSize(150, 40);

        base.add(launch);
        launch.setLocation(440, 320);
        launch.setSize(150, 40);

        base.add(profilesLabel);
        profilesLabel.setLocation(440, 155);
        profilesLabel.setSize(65, 20);

        base.add(profiles);
        profiles.setLocation(440, 180);
        profiles.setSize(150, 23);

        base.add(profileDetailLabel);
        profileDetailLabel.setLocation(440, 210);
        profileDetailLabel.setSize(150, 100);

        //================ profile panel ====================

        base.add(profilePanel);
        profilePanel.setLocation(0, 12);
        profilePanel.setSize(430, 325);
        profilePanel.setLayout(null);
        profilePanel.setVisible(false);

        JLabel profilesLabel2 = new JLabel(Lang.getString("ui.profile.label"));
        JComboBox<Profile> profiles2 = new JComboBox<Profile>();

        profilePanel.add(profilesLabel2);
        profilesLabel2.setLocation(110, 10);
        profilesLabel2.setSize(65, 20);

        profilePanel.add(profiles2);
        profiles2.setLocation(175, 10);
        profiles2.setSize(150, 23);
        profiles2.setModel(profiles.getModel());

        profilePanel.add(addProfile);
        addProfile.setLocation(105, 40);
        addProfile.setSize(100, 30);

        profilePanel.add(removeProfile);
        removeProfile.setLocation(225, 40);
        removeProfile.setSize(100, 30);

        profilePanel.add(userLabel);
        userLabel.setLocation(105, 78);
        userLabel.setSize(200, 20);

        profilePanel.add(user);
        user.setLocation(105, 98);
        user.setSize(220, 25);

        profilePanel.add(passLabel);
        passLabel.setLocation(105, 125);
        passLabel.setSize(200, 20);

        profilePanel.add(pass);
        pass.setLocation(105, 145);
        pass.setSize(220, 25);
        pass.addFocusListener(new FocusAdapter(){
            @Override
            public void focusGained(FocusEvent e) {
                pass.setSelectionStart(0);
                pass.setSelectionEnd(pass.getText().length());
            }
        });

        profilePanel.add(savePass);
        savePass.setLocation(105, 173);
        savePass.setSize(190, 20);

        profilePanel.add(authType);
        authType.setLocation(185, 208);
        authType.setSize(140, 23);
        for(AuthType at : AuthType.values()) {
            authType.addItem(at);
        }

        profilePanel.add(authTypeLabel);
        authTypeLabel.setLocation(105, 208);
        authTypeLabel.setSize(80, 20);

        profilePanel.add(gameVersion);
        gameVersion.setLocation(185, 236);
        gameVersion.setSize(140, 23);

        profilePanel.add(gameVersionLabel);
        gameVersionLabel.setLocation(105, 236);
        gameVersionLabel.setSize(80, 20);

        profilePanel.add(runPathLabel);
        runPathLabel.setLocation(105, 275);
        runPathLabel.setSize(200, 20);

        profilePanel.add(runPath);
        runPath.setLocation(105, 300);
        runPath.setSize(190, 25);

        profilePanel.add(runPathSearch);
        runPathSearch.setLocation(300, 300);
        runPathSearch.setSize(25, 25);
        runPathSearch.addActionListener(new ActionListener() {
            private JFileChooser fc = new JFileChooser();
            public void actionPerformed(ActionEvent e) {
                fc.setCurrentDirectory(new File(runPath.getText()));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.showDialog(LauncherFrame.this, Lang.getString("ui.runpath.filechooser.title"));
                if(fc.getSelectedFile() != null)
                    runPath.setText(fc.getSelectedFile().getPath());
            }
        });

        //================ module panel ====================

        base.add(modulePanel);
        modulePanel.setLocation(0, 0);
        modulePanel.setSize(430, 350);
        modulePanel.setLayout(null);
        modulePanel.setVisible(false);

        modulePanel.add(modulesLabel);
        modulesLabel.setLocation(5, 0);
        modulesLabel.setSize(300, 20);

        modulePanel.add(modulesOuter);
        modulesOuter.setLocation(0, 20);
        modulesOuter.setSize(430, 290);
        modulesModel.addColumn(Lang.getString("ui.table.name"));
        modulesModel.addColumn(Lang.getString("ui.table.type"));
        modulesModel.addColumn(Lang.getString("ui.table.state"));
        modules.getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(125);
        modules.getTableHeader().getColumnModel().getColumn(1).setPreferredWidth(75);
        modules.getTableHeader().getColumnModel().getColumn(2).setPreferredWidth(75);
        modules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        modulePanel.add(installModules);
        installModules.setLocation(5, 320);
        installModules.setSize(90, 30);

        modulePanel.add(uninstallModules);
        uninstallModules.setLocation(100, 320);
        uninstallModules.setSize(90, 30);

        modulePanel.add(showOld);
        showOld.setLocation(195, 322);
        showOld.setSize(100, 25);

        modulePanel.add(showSnapshot);
        showSnapshot.setLocation(295, 322);
        showSnapshot.setSize(100, 25);

        //================ system panel ====================

        base.add(systemPanel);
        systemPanel.setLocation(100, 40);
        systemPanel.setSize(230, 260);
        systemPanel.setLayout(null);
        systemPanel.setVisible(false);

        runningMode.add(runningMode32);
        runningMode.add(runningMode64);
        runningMode.add(runningModeDefault);

        systemPanel.add(runningMode32);
        runningMode32.setLocation(5, 55);
        runningMode32.setSize(62, 20);

        systemPanel.add(runningMode64);
        runningMode64.setLocation(77, 55);
        runningMode64.setSize(63, 20);

        systemPanel.add(runningModeDefault);
        runningModeDefault.setLocation(150, 55);
        runningModeDefault.setSize(70, 20);

        systemPanel.add(jrePathLabel);
        jrePathLabel.setLocation(5, 0);
        jrePathLabel.setSize(200, 20);

        systemPanel.add(jrePath);
        jrePath.setLocation(5, 25);
        jrePath.setSize(190, 25);

        systemPanel.add(jrePathSearch);
        jrePathSearch.setLocation(200, 25);
        jrePathSearch.setSize(25, 25);
        jrePathSearch.addActionListener(new ActionListener() {
            private JFileChooser fc = new JFileChooser();
            public void actionPerformed(ActionEvent e) {
                fc.setCurrentDirectory(new File(jrePath.getText()));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.showDialog(LauncherFrame.this, Lang.getString("ui.jrepath.filechooser.title"));
                if(fc.getSelectedFile() != null)
                    jrePath.setText(fc.getSelectedFile().getPath());
            }
        });

        systemPanel.add(memorySizeLabel);
        memorySizeLabel.setLocation(5, 85);
        memorySizeLabel.setSize(110, 20);

        systemPanel.add(memorySize);
        memorySize.setLocation(115, 85);
        memorySize.setSize(110, 25);

        systemPanel.add(memorySizeSlider);
        memorySizeSlider.setLocation(5, 110);
        memorySizeSlider.setSize(220, 38);
        memorySizeSlider.setMinimum(0);
        memorySizeSlider.setMaximum(8192);
        memorySizeSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                memorySize.setText(String.valueOf(memorySizeSlider.getValue()));
            }
        });

        systemPanel.add(enableProxy);
        enableProxy.setLocation(5, 160);
        enableProxy.setSize(220, 20);

        systemPanel.add(proxyTypeLabel);
        proxyTypeLabel.setLocation(5, 185);
        proxyTypeLabel.setSize(100, 20);

        systemPanel.add(proxyType);
        proxyType.setLocation(105, 185);
        proxyType.setSize(120, 20);

        systemPanel.add(proxyHostPortLabel);
        proxyHostPortLabel.setLocation(5, 210);
        proxyHostPortLabel.setSize(220, 20);

        systemPanel.add(proxy);
        proxy.setLocation(5, 235);
        proxy.setSize(220, 20);
    }

    public void setStdOut() {
        if(thisStdOut != null && thisStdOut == System.out) {
            System.setOut(oldStdOut);
            if(Config.showDebugInfo) {
                System.setErr(oldStdErr);
            }
        } else {
            thisStdOut = new PrintStream(new ConsoleOutputStream(), true);
            oldStdOut = System.out;
            oldStdErr = System.err;
            System.setOut(thisStdOut);
            if(Config.showDebugInfo) {
                System.setErr(thisStdOut);
            }
        }
    }

    public void outputConsole(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                commentLabel.setText(message);
            }
        });
    }

    class ConsoleOutputStream extends OutputStream {

        final byte[] buffer = new byte[65536];
        int pos = 0;

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            synchronized (buffer) {
                super.write(b, off, len);
            }
        }

        public void write(int b) throws IOException {
            synchronized (buffer) {
                if(b == 13) {
                    String message = new String(buffer, 0, pos);
                    outputConsole(message);
                    if(oldStdOut != null) {
                        oldStdOut.print(message);
                    }
                    pos = 0;
                } else {
                    buffer[pos] = (byte) b;
                    pos++;
                    if(pos >= buffer.length) {
                        pos = 0;
                    }
                }
            }
        }
    }

    class UneditableTableModel extends DefaultTableModel {

        private static final long serialVersionUID = -2140422989547704774L;

        @Override
        public boolean isCellEditable(int col, int row) {
            return false;
        }
    }

    class LauncherFrameFocusTraversalPolicy extends FocusTraversalPolicy {

        public List<Component> componentList = new ArrayList<Component>();

        public LauncherFrameFocusTraversalPolicy() {
            componentList.add(profiles);
            componentList.add(addProfile);
            componentList.add(removeProfile);
            componentList.add(user);
            componentList.add(pass);
            componentList.add(savePass);
            componentList.add(authType);
            componentList.add(gameVersion);
            componentList.add(runPath);
            componentList.add(runPathSearch);
            componentList.add(launch);
            componentList.add(jrePath);
            componentList.add(jrePathSearch);
            componentList.add(runningMode32);
            componentList.add(runningMode64);
            componentList.add(runningModeDefault);
            componentList.add(memorySize);
            componentList.add(memorySizeSlider);
        }

        public Component getComponentAfter(Container aContainer,
                Component aComponent) {
            int i = componentList.indexOf(aComponent);
            return componentList.get((i + 1) % componentList.size());
        }

        public Component getComponentBefore(Container aContainer,
                Component aComponent) {
            int i = componentList.indexOf(aComponent);
            return componentList.get((i + componentList.size() - 1) % componentList.size());
        }

        public Component getFirstComponent(Container aContainer) {
            return componentList.get(0);
        }

        public Component getLastComponent(Container aContainer) {
            return componentList.get(componentList.size() - 1);
        }

        public Component getDefaultComponent(Container aContainer) {
            return componentList.get(0);
        }

    }
}
