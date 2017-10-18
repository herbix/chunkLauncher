package io.chaofan.chunklauncher;

import java.awt.*;
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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import io.chaofan.chunklauncher.directory.*;
import io.chaofan.chunklauncher.util.HttpFetcher;
import io.chaofan.chunklauncher.auth.AuthType;
import io.chaofan.chunklauncher.download.Downloader;
import io.chaofan.chunklauncher.util.Lang;
import io.chaofan.chunklauncher.util.UI;

public class LauncherFrame extends JFrame {

    private static final long serialVersionUID = -1923022699772187651L;

    JPanel base = new JPanel();

    JPanel cardPanel = new JPanel();
    CardLayout cards = new CardLayout();
    String selectedCard = "profile";

    JLabel modulesLabel = new JLabel(Lang.getString("ui.module.label"));
    DefaultTableModel modulesModel = new UneditableTableModel();
    JTable modules = new JTable(modulesModel);
    JScrollPane modulesOuter = new JScrollPane(modules);
    JButton installModules = new JButton(Lang.getString("ui.module.install"));
    JButton uninstallModules = new JButton(Lang.getString("ui.module.uninstall"));
    JCheckBox showOld = new JCheckBox(Lang.getString("ui.module.old"));
    JCheckBox showSnapshot = new JCheckBox(Lang.getString("ui.module.snapshot"));

    JComboBox<Profile> profiles2 = new JComboBox<Profile>();
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
    JComboBox<RunningDirectory> runPathDirectories = new JComboBox<RunningDirectory>();

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

    JComboBox<RunningDirectory> directories = new JComboBox<RunningDirectory>();
    JButton addDirectory = new JButton(Lang.getString("ui.directory.add"));
    JButton removeDirectory = new JButton(Lang.getString("ui.directory.remove"));
    JLabel directoryPathLabel = new JLabel(Lang.getString("ui.directory.pathlabel"));
    JTextField directoryPath = new JTextField();
    JButton directoryPathSearch = new JButton("...");
    JTabbedPane directoryManages = new JTabbedPane();

    JButton profileSetting = new JButton(Lang.getString("ui.tab.profile"));
    JButton moduleSetting = new JButton(Lang.getString("ui.tab.module"));
    JButton directorySetting = new JButton(Lang.getString("ui.tab.directory"));
    JButton systemSetting = new JButton(Lang.getString("ui.tab.system"));
    JPanel profilePanel = new JPanel();
    JPanel modulePanel = new JPanel();
    JPanel directoryPanel = new JPanel();
    JPanel systemPanel = new JPanel();

    PrintStream thisStdOut = null;
    PrintStream oldStdOut = null;

    private PrintStream oldStdErr;

    public LauncherFrame() {
        if (Launcher.undecoratedFrame) {
            setUndecorated(true);
            this.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
        }
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

        setTitle("ChunkLauncher for Minecraft V" + Launcher.VERSION + " (Made by Chaofan)");
        setIconImage(new ImageIcon(getClass().getResource("/favicon.png")).getImage());
        base.setPreferredSize(new Dimension(600, 450));
        add(base);
        pack();
        setLocationByPlatform(true);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);

        base.setLayout(new BorderLayout());
        createFrame();
        base.getRootPane().setDefaultButton(launch);

        this.setFocusTraversalPolicy(new LauncherFrameFocusTraversalPolicy());
    }

    private void createFrame() {
        JPanel topPart = new JPanel();
        topPart.setLayout(new GridBagLayout());
        base.add(topPart, BorderLayout.NORTH);

        topPart.add(profileSetting, UI.gbc(0, 0, UI.insets(5, 5, 0, 0)));
        profileSetting.setPreferredSize(new Dimension(130, 35));

        topPart.add(moduleSetting, UI.gbc(1, 0, UI.insets(5, 5, 0, 0)));
        moduleSetting.setPreferredSize(new Dimension(130, 35));

        topPart.add(directorySetting, UI.gbc(2, 0, UI.insets(5, 5, 0, 0)));
        directorySetting.setPreferredSize(new Dimension(130, 35));

        topPart.add(systemSetting, UI.gbc(3, 0, UI.insets(5, 5, 0, 0)));
        systemSetting.setPreferredSize(new Dimension(130, 35));

        JPanel bottomPart = new JPanel();
        bottomPart.setLayout(new GridBagLayout());
        base.add(bottomPart, BorderLayout.SOUTH);

        bottomPart.add(new JSeparator(), UI.gbc(0, 0, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.HORIZONTAL, UI.insets(2, 5)));

        bottomPart.add(profilesLabel, UI.gbc(0, 1, 0, 1, GridBagConstraints.BOTH, UI.insets(0, 5)));
        profilesLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        profilesLabel.setPreferredSize(new Dimension(65, 20));

        bottomPart.add(profiles, UI.gbc(0, 2, 0, 1, GridBagConstraints.HORIZONTAL, UI.insets(5)));
        profiles.setPreferredSize(new Dimension(150, 23));

        bottomPart.add(profileDetailLabel, UI.gbc(2, 1, 1, 2, 1, 0, GridBagConstraints.HORIZONTAL, UI.insets(5)));
        profileDetailLabel.setPreferredSize(new Dimension(10, 50));

        bottomPart.add(launch, UI.gbc(3, 1, 1, 2, 0, 0, GridBagConstraints.BOTH, UI.insets(5)));
        launch.setPreferredSize(new Dimension(150, 35));

        bottomPart.add(commentLabel, UI.gbc(0, 3, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        commentLabel.setPreferredSize(new Dimension(10, 20));

        bottomPart.add(progress, UI.gbc(0, 4, GridBagConstraints.REMAINDER, 1, 1, 0, GridBagConstraints.HORIZONTAL));
        progress.setPreferredSize(new Dimension(10, 20));
        HttpFetcher.setJProgressBar(progress);

        cardPanel.setLayout(cards);
        cardPanel.setPreferredSize(new Dimension(430, 350));
        base.add(cardPanel, BorderLayout.CENTER);

        //================ profile panel ====================

        cardPanel.add(profilePanel, "profile");
        profilePanel.setLayout(new GridBagLayout());
        profilePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

        JPanel profilesComboBoxLine = new JPanel();
        profilesComboBoxLine.setLayout(new GridBagLayout());
        profilePanel.add(profilesComboBoxLine, UI.gbc(0, 0, 1, 0, GridBagConstraints.HORIZONTAL));

        JLabel profilesLabel2 = new JLabel(Lang.getString("ui.profile.label"));

        profilesComboBoxLine.add(profilesLabel2, UI.gbc(0, 0, UI.insets(0, 5, 0, 0)));
        profilesLabel2.setPreferredSize(new Dimension(80, 20));

        profilesComboBoxLine.add(profiles2, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        profiles2.setPreferredSize(new Dimension(10, 23));
        profiles2.setModel(profiles.getModel());

        profilesComboBoxLine.add(addProfile, UI.gbc(2, 0, UI.insets(5)));
        addProfile.setPreferredSize(new Dimension(100, 30));

        profilesComboBoxLine.add(removeProfile, UI.gbc(3, 0, UI.insets(5, 0)));
        removeProfile.setPreferredSize(new Dimension(100, 30));

        JPanel userLine = new JPanel();
        userLine.setLayout(new GridBagLayout());
        profilePanel.add(userLine, UI.gbc(0, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        userLine.add(userLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        userLabel.setPreferredSize(new Dimension(80, 20));

        userLine.add(user, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        user.setPreferredSize(new Dimension(10, 25));

        JPanel passwordLine = new JPanel();
        passwordLine.setLayout(new GridBagLayout());
        profilePanel.add(passwordLine, UI.gbc(0, 2, 1, 0, GridBagConstraints.HORIZONTAL));

        passwordLine.add(passLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        passLabel.setPreferredSize(new Dimension(80, 20));

        passwordLine.add(pass, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        pass.setPreferredSize(new Dimension(10, 25));
        pass.addFocusListener(new FocusAdapter(){
            @Override
            public void focusGained(FocusEvent e) {
                pass.setSelectionStart(0);
                pass.setSelectionEnd(pass.getText().length());
            }
        });

        profilePanel.add(savePass, UI.gbc(0, 3, 1, 0, GridBagConstraints.HORIZONTAL, UI.insets(0, 85, 5, 0)));
        savePass.setPreferredSize(new Dimension(10, 20));

        JPanel authTypeLine = new JPanel();
        authTypeLine.setLayout(new GridBagLayout());
        profilePanel.add(authTypeLine, UI.gbc(0, 4, 1, 0, GridBagConstraints.HORIZONTAL));

        authTypeLine.add(authTypeLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        authTypeLabel.setPreferredSize(new Dimension(80, 20));

        authTypeLine.add(authType, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        authType.setPreferredSize(new Dimension(10, 23));
        for(AuthType at : AuthType.values()) {
            authType.addItem(at);
        }

        JPanel gameVersionLine = new JPanel();
        gameVersionLine.setLayout(new GridBagLayout());
        profilePanel.add(gameVersionLine, UI.gbc(0, 5, 1, 0, GridBagConstraints.HORIZONTAL));

        gameVersionLine.add(gameVersionLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        gameVersionLabel.setPreferredSize(new Dimension(80, 20));

        gameVersionLine.add(gameVersion, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        gameVersion.setPreferredSize(new Dimension(10, 23));

        JPanel runPathLine = new JPanel();
        runPathLine.setLayout(new GridBagLayout());
        profilePanel.add(runPathLine, UI.gbc(0, 6, 1, 0, GridBagConstraints.HORIZONTAL));

        runPathLine.add(runPathLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        runPathLabel.setPreferredSize(new Dimension(200, 20));

        runPathLine.add(runPathDirectories, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        runPathDirectories.setPreferredSize(new Dimension(10, 25));

        profilePanel.add(new JPanel(), UI.gbc(0, 100, 1, 1, GridBagConstraints.BOTH));

        //================ module panel ====================

        cardPanel.add(modulePanel, "module");
        modulePanel.setLayout(new BorderLayout());

        modulePanel.add(modulesLabel, BorderLayout.NORTH);
        modulesLabel.setLocation(5, 0);
        modulesLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        modulesLabel.setPreferredSize(new Dimension(300, 20));

        modulePanel.add(modulesOuter);
        modulesModel.addColumn(Lang.getString("ui.table.name"));
        modulesModel.addColumn(Lang.getString("ui.table.type"));
        modulesModel.addColumn(Lang.getString("ui.table.state"));
        modules.getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(125);
        modules.getTableHeader().getColumnModel().getColumn(1).setPreferredWidth(75);
        modules.getTableHeader().getColumnModel().getColumn(2).setPreferredWidth(75);
        modules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel moduleBottomPart = new JPanel();
        moduleBottomPart.setLayout(null);
        moduleBottomPart.setPreferredSize(new Dimension(300, 50));
        modulePanel.add(moduleBottomPart, BorderLayout.SOUTH);

        moduleBottomPart.add(installModules);
        installModules.setLocation(5, 10);
        installModules.setSize(90, 30);

        moduleBottomPart.add(uninstallModules);
        uninstallModules.setLocation(100, 10);
        uninstallModules.setSize(90, 30);

        moduleBottomPart.add(showOld);
        showOld.setLocation(195, 12);
        showOld.setSize(100, 25);

        moduleBottomPart.add(showSnapshot);
        showSnapshot.setLocation(295, 12);
        showSnapshot.setSize(100, 25);

        //================ directory panel ====================

        cardPanel.add(directoryPanel, "directory");
        directoryPanel.setLayout(new GridBagLayout());
        directoryPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JPanel directoriesComboBoxLine = new JPanel();
        directoriesComboBoxLine.setLayout(new GridBagLayout());
        directoryPanel.add(directoriesComboBoxLine, UI.gbc(0, 0, 1, 0, GridBagConstraints.BOTH));

        JLabel directoriesLabel = new JLabel(Lang.getString("ui.directory.label"));

        directoriesComboBoxLine.add(directoriesLabel, UI.gbc(0, 0, UI.insets(5, 0)));
        directoriesLabel.setPreferredSize(new Dimension(80, 20));

        directoriesComboBoxLine.add(directories, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        directories.setPreferredSize(new Dimension(10, 23));

        directoriesComboBoxLine.add(addDirectory, UI.gbc(2, 0, UI.insets(5)));
        addDirectory.setPreferredSize(new Dimension(100, 30));

        directoriesComboBoxLine.add(removeDirectory, UI.gbc(3, 0, UI.insets(5, 0)));
        removeDirectory.setPreferredSize(new Dimension(100, 30));

        JPanel runPathLine2 = new JPanel();
        runPathLine2.setLayout(new GridBagLayout());
        directoryPanel.add(runPathLine2, UI.gbc(0, 1, 1, 0, GridBagConstraints.BOTH));

        runPathLine2.add(directoryPathLabel, UI.gbc(0, 0, UI.insets(5, 0)));
        directoryPathLabel.setPreferredSize(new Dimension(80, 20));

        runPathLine2.add(directoryPath, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        directoryPath.setPreferredSize(new Dimension(10, 25));

        runPathLine2.add(directoryPathSearch, UI.gbc(2, 0, UI.insets(5, 0)));
        directoryPathSearch.setPreferredSize(new Dimension(25, 25));
        directoryPathSearch.addActionListener(new ActionListener() {
            private JFileChooser fc = new JFileChooser();
            public void actionPerformed(ActionEvent e) {
                fc.setCurrentDirectory(new File(directoryPath.getText()));
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.showDialog(LauncherFrame.this, Lang.getString("ui.runpath.filechooser.title"));
                if(fc.getSelectedFile() != null)
                    directoryPath.setText(fc.getSelectedFile().getPath());
            }
        });

        directoryPanel.add(directoryManages, UI.gbc(0, 2, 1, 1, GridBagConstraints.BOTH, UI.insets(5, -5, 0, -5)));
        directoryManages.setPreferredSize(new Dimension(10, 10));

        directoryPath.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                File directory = new File(directoryPath.getText());
                if (!directory.isAbsolute()) {
                    directory = new File(Config.gamePath, directoryPath.getText());
                }
                directoryManages.removeAll();
                directoryManages.setEnabled(true);
                File saves = new File(directory, "saves");
                if (saves.isDirectory()) {
                    directoryManages.addTab(Lang.getString("ui.directory.saves"),
                            DirectoryTabPanel.createDirectoryTab(LauncherFrame.this, DirectoryType.SAVE,
                                    DirectoryTabPanel.COPY | DirectoryTabPanel.REMOVE, saves));
                }
                File resourcePacks = new File(directory, "resourcepacks");
                if (resourcePacks.isDirectory()) {
                    directoryManages.addTab(Lang.getString("ui.directory.resourcepacks"),
                            DirectoryTabPanel.createDirectoryTab(LauncherFrame.this, DirectoryType.PACK,
                                    DirectoryTabPanel.INSTALL | DirectoryTabPanel.REMOVE, resourcePacks));
                }
                File texturePacks = new File(directory, "texturepacks");
                if (texturePacks.isDirectory()) {
                    directoryManages.addTab(Lang.getString("ui.directory.texturepacks"),
                            DirectoryTabPanel.createDirectoryTab(LauncherFrame.this, DirectoryType.PACK,
                                    DirectoryTabPanel.INSTALL | DirectoryTabPanel.REMOVE, texturePacks));
                }
                File shaderPacks = new File(directory, "shaderpacks");
                if (shaderPacks.isDirectory()) {
                    directoryManages.addTab(Lang.getString("ui.directory.shaderpacks"),
                            DirectoryTabPanel.createDirectoryTab(LauncherFrame.this, DirectoryType.PACK,
                                    DirectoryTabPanel.INSTALL | DirectoryTabPanel.REMOVE, shaderPacks));
                }
                File mods = new File(directory, "mods");
                if (mods.isDirectory()) {
                    directoryManages.addTab(Lang.getString("ui.directory.mods"),
                            DirectoryTabPanel.createDirectoryTab(LauncherFrame.this, DirectoryType.MOD,
                                    DirectoryTabPanel.INSTALL | DirectoryTabPanel.ENABLE |
                                    DirectoryTabPanel.DISABLE | DirectoryTabPanel.REMOVE, mods));
                }

                if (directoryManages.getTabCount() == 0) {
                    directoryManages.addTab(Lang.getString("ui.directory.saves"), new JPanel());
                    directoryManages.setEnabled(false);
                }
            }
        });

        //================ system panel ====================

        cardPanel.add(systemPanel, "system");
        systemPanel.setLayout(new GridBagLayout());
        systemPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));

        JPanel jrePathLine = new JPanel();
        jrePathLine.setLayout(new GridBagLayout());
        systemPanel.add(jrePathLine, UI.gbc(0, 0, 1, 0, GridBagConstraints.HORIZONTAL));

        jrePathLine.add(jrePathLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        jrePathLabel.setPreferredSize(new Dimension(180, 20));

        jrePathLine.add(jrePath, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        jrePath.setPreferredSize(new Dimension(10, 25));

        jrePathLine.add(jrePathSearch, UI.gbc(2, 0, UI.insets(5, 5, 5, 0)));
        jrePathSearch.setPreferredSize(new Dimension(25, 25));
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

        JPanel archLine = new JPanel();
        archLine.setLayout(new GridBagLayout());
        systemPanel.add(archLine, UI.gbc(0, 1, 1, 0, GridBagConstraints.HORIZONTAL));

        runningMode.add(runningMode32);
        runningMode.add(runningMode64);
        runningMode.add(runningModeDefault);

        archLine.add(runningMode32, UI.gbc(0, 0, 1, 0, GridBagConstraints.HORIZONTAL, UI.insets(5, 180, 5, 0)));
        runningMode32.setPreferredSize(new Dimension(62, 20));

        archLine.add(runningMode64, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        runningMode64.setPreferredSize(new Dimension(63, 20));

        archLine.add(runningModeDefault, UI.gbc(2, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        runningModeDefault.setPreferredSize(new Dimension(70, 20));

        JPanel memoryLine = new JPanel();
        memoryLine.setLayout(new GridBagLayout());
        systemPanel.add(memoryLine, UI.gbc(0, 2, 1, 0, GridBagConstraints.HORIZONTAL));

        memoryLine.add(memorySizeLabel, UI.gbc(0, 0, UI.insets(5)));
        memorySizeLabel.setPreferredSize(new Dimension(175, 20));

        memoryLine.add(memorySize, UI.gbc(2, 0, UI.insets(5, 5, 5, 0)));
        memorySize.setPreferredSize(new Dimension(110, 25));

        memoryLine.add(memorySizeSlider, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        memorySizeSlider.setPreferredSize(new Dimension(10, 38));
        memorySizeSlider.setMinimum(0);
        memorySizeSlider.setMaximum(8192);
        memorySizeSlider.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                memorySize.setText(String.valueOf(memorySizeSlider.getValue()));
            }
        });

        systemPanel.add(enableProxy, UI.gbc(0, 3, 1, 0, GridBagConstraints.HORIZONTAL, UI.insets(5)));
        enableProxy.setPreferredSize(new Dimension(10, 20));

        JPanel proxyTypeLine = new JPanel();
        proxyTypeLine.setLayout(new GridBagLayout());
        systemPanel.add(proxyTypeLine, UI.gbc(0, 4, 1, 0, GridBagConstraints.HORIZONTAL));

        proxyTypeLine.add(proxyTypeLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        proxyTypeLabel.setPreferredSize(new Dimension(180, 20));

        proxyTypeLine.add(proxyType, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        proxyType.setPreferredSize(new Dimension(10, 25));

        JPanel proxyLine = new JPanel();
        proxyLine.setLayout(new GridBagLayout());
        systemPanel.add(proxyLine, UI.gbc(0, 5, 1, 0, GridBagConstraints.HORIZONTAL));

        proxyLine.add(proxyHostPortLabel, UI.gbc(0, 0, UI.insets(5, 5, 5, 0)));
        proxyHostPortLabel.setPreferredSize(new Dimension(180, 20));

        proxyLine.add(proxy, UI.gbc(1, 0, 1, 0, GridBagConstraints.HORIZONTAL));
        proxy.setPreferredSize(new Dimension(10, 25));

        systemPanel.add(new JPanel(), UI.gbc(0, 100, 1, 1, GridBagConstraints.BOTH));
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

    class LauncherFrameFocusTraversalPolicy extends FocusTraversalPolicy {

        public List<Component> componentListProfile = new ArrayList<Component>();
        public List<Component> componentListModule = new ArrayList<Component>();
        public List<Component> componentListDirectory = new ArrayList<Component>();
        public List<Component> componentListSystem = new ArrayList<Component>();

        public LauncherFrameFocusTraversalPolicy() {
            initPre(componentListProfile);
            componentListProfile.add(profiles2);
            componentListProfile.add(addProfile);
            componentListProfile.add(removeProfile);
            componentListProfile.add(user);
            componentListProfile.add(pass);
            componentListProfile.add(savePass);
            componentListProfile.add(authType);
            componentListProfile.add(gameVersion);
            componentListProfile.add(runPathDirectories);
            initPost(componentListProfile);

            initPre(componentListModule);
            componentListModule.add(modules);
            componentListModule.add(installModules);
            componentListModule.add(uninstallModules);
            componentListModule.add(showOld);
            componentListModule.add(showSnapshot);
            initPost(componentListModule);

            initPre(componentListDirectory);
            componentListDirectory.add(directories);
            componentListDirectory.add(addDirectory);
            componentListDirectory.add(removeDirectory);
            componentListDirectory.add(directoryPath);
            componentListDirectory.add(directoryPathSearch);
            componentListDirectory.add(directoryManages);
            initPost(componentListDirectory);

            initPre(componentListSystem);
            componentListSystem.add(jrePath);
            componentListSystem.add(jrePathSearch);
            componentListSystem.add(runningMode32);
            componentListSystem.add(runningMode64);
            componentListSystem.add(runningModeDefault);
            componentListSystem.add(memorySizeSlider);
            componentListSystem.add(memorySize);
            componentListSystem.add(enableProxy);
            componentListSystem.add(proxyType);
            componentListSystem.add(proxy);
            initPost(componentListSystem);
        }

        private void initPre(List<Component> componentListProfileLocal) {
            componentListProfileLocal.add(profileSetting);
            componentListProfileLocal.add(moduleSetting);
            componentListProfileLocal.add(directorySetting);
            componentListProfileLocal.add(systemSetting);
        }

        private void initPost(List<Component> componentListProfileLocal) {
            componentListProfileLocal.add(profiles);
            componentListProfileLocal.add(launch);
        }

        private List<Component> getCurrentComponentList() {
            switch (selectedCard) {
                case "profile":
                    return componentListProfile;
                case "module":
                    return componentListModule;
                case "directory":
                    return componentListDirectory;
                case "system":
                    return componentListSystem;
            }
            return componentListProfile;
        }

        public Component getComponentAfter(Container aContainer,
                                           Component aComponent) {
            List<Component> componentList = getCurrentComponentList();
            int i = componentList.indexOf(aComponent);
            return componentList.get((i + 1) % componentList.size());
        }

        public Component getComponentBefore(Container aContainer,
                                            Component aComponent) {
            List<Component> componentList = getCurrentComponentList();
            int i = componentList.indexOf(aComponent);
            return componentList.get((i + componentList.size() - 1) % componentList.size());
        }

        public Component getFirstComponent(Container aContainer) {
            List<Component> componentList = getCurrentComponentList();
            return componentList.get(0);
        }

        public Component getLastComponent(Container aContainer) {
            List<Component> componentList = getCurrentComponentList();
            return componentList.get(componentList.size() - 1);
        }

        public Component getDefaultComponent(Container aContainer) {
            List<Component> componentList = getCurrentComponentList();
            return componentList.get(0);
        }

    }
}
