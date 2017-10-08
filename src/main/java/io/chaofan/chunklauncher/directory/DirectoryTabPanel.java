package io.chaofan.chunklauncher.directory;

import io.chaofan.chunklauncher.UneditableTableModel;
import io.chaofan.chunklauncher.util.EasyFileAccess;
import io.chaofan.chunklauncher.util.Lang;
import io.chaofan.chunklauncher.util.UI;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Method;

public class DirectoryTabPanel extends JPanel {

    public static final int INSTALL = 1;
    public static final int ENABLE = 2;
    public static final int DISABLE = 4;
    public static final int COPY = 8;
    public static final int REMOVE = 16;

    private final JFrame frame;
    private DirectoryType type;

    private DefaultTableModel tableModel = new UneditableTableModel();
    private JTable table = new JTable(tableModel);
    private JScrollPane tableOuter = new JScrollPane(table);
    private JButton install = new JButton(Lang.getString("ui.directory.tab.install"));
    private JButton enable = new JButton(Lang.getString("ui.directory.tab.enable"));
    private JButton disable = new JButton(Lang.getString("ui.directory.tab.disable"));
    private JButton copy = new JButton(Lang.getString("ui.directory.tab.copy"));
    private JButton remove = new JButton(Lang.getString("ui.directory.tab.remove"));

    private File directory;

    public DirectoryTabPanel(JFrame frame, DirectoryType type, final int flags) {
        setOpaque(false);
        this.frame = frame;
        this.type = type;
        createPanel(flags);
        initListeners();
        initializeColumn();
    }

    private void createPanel(int flags) {
        install.setVisible((flags & INSTALL) != 0);
        enable.setVisible((flags & ENABLE) != 0);
        disable.setVisible((flags & DISABLE) != 0);
        copy.setVisible((flags & COPY) != 0);
        remove.setVisible((flags & REMOVE) != 0);

        setLayout(new BorderLayout());

        add(tableOuter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel rightPart = new JPanel();
        rightPart.setOpaque(false);
        rightPart.setLayout(new GridBagLayout());
        add(rightPart, BorderLayout.EAST);

        rightPart.add(install, UI.gbc(0, 0, UI.insets(5, 10)));
        install.setPreferredSize(new Dimension(100, 35));

        rightPart.add(enable, UI.gbc(0, 1, UI.insets(5, 10)));
        enable.setPreferredSize(new Dimension(100, 35));

        rightPart.add(disable, UI.gbc(0, 2, UI.insets(5, 10)));
        disable.setPreferredSize(new Dimension(100, 35));

        rightPart.add(copy, UI.gbc(0, 3, UI.insets(5, 10)));
        copy.setPreferredSize(new Dimension(100, 35));

        rightPart.add(remove, UI.gbc(0, 4, UI.insets(5, 10)));
        remove.setPreferredSize(new Dimension(100, 35));

        JPanel padding = new JPanel();
        padding.setOpaque(false);
        rightPart.add(padding, UI.gbc(0, 5, 1, 1, GridBagConstraints.BOTH));
    }

    private void initListeners() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }
                Object object = table.getValueAt(row, 0);
                if (object instanceof IEnableProvider) {
                    enable.setEnabled(!((IEnableProvider) object).isEnabled());
                    disable.setEnabled(((IEnableProvider) object).isEnabled());
                }
            }
        });

        enable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ITableRowProvider rowProvider = getSelectedProvider();
                if (rowProvider == null) {
                    return;
                }
                File f = rowProvider.getFile();
                String path = f.getPath();
                f.renameTo(new File(path.substring(0, path.length() - 9)));
                refresh();
            }
        });

        disable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ITableRowProvider rowProvider = getSelectedProvider();
                if (rowProvider == null) {
                    return;
                }
                File f = rowProvider.getFile();
                String path = f.getPath();
                f.renameTo(new File(path + ".disabled"));
                refresh();
            }
        });

        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ITableRowProvider rowProvider = getSelectedProvider();
                if (rowProvider == null) {
                    return;
                }
                int r = JOptionPane.showConfirmDialog(frame, Lang.getString("msg.directory.tab.removeconfirm"),
                        "ChunkLauncher", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if(r != JOptionPane.YES_OPTION) {
                    return;
                }
                EasyFileAccess.deleteFileForce(rowProvider.getFile());
                refresh();
            }
        });

        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ITableRowProvider rowProvider = getSelectedProvider();
                if (rowProvider == null) {
                    return;
                }
                File file = rowProvider.getFile();
                String name = (String) JOptionPane.showInputDialog(frame, Lang.getString("msg.directory.tab.inputname"),
                        "ChunkLauncher", JOptionPane.QUESTION_MESSAGE, null, null, file.getName());
                if(name == null || name.equals("")) {
                    return;
                }
                File targetFile = new File(directory, name);
                if (targetFile.exists()) {
                    System.out.println(Lang.getString("msg.directory.tab.existed"));
                    return;
                }
                if (!EasyFileAccess.copyDirectory(file, targetFile)) {
                    System.out.println(Lang.getString("msg.directory.tab.copyfailed"));
                }
                refresh();
            }
        });

        install.addActionListener(new ActionListener() {
            private JFileChooser fc = new JFileChooser();
            @Override
            public void actionPerformed(ActionEvent e) {
                fc.setFileFilter(type.getInstallFilter());
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.showOpenDialog(frame);
                if (fc.getSelectedFile() != null) {
                    File source = fc.getSelectedFile();
                    EasyFileAccess.copyFile(source, new File(directory, source.getName()));
                    refresh();
                }
            }
        });
    }

    private ITableRowProvider getSelectedProvider() {
        ITableRowProvider rowProvider = null;
        int row = table.getSelectedRow();
        if (row < 0) {
            System.out.println(Lang.getString("msg.directory.pleaseselectanitem"));
        } else {
            rowProvider = (ITableRowProvider) table.getValueAt(row, 0);
        }
        return rowProvider;
    }

    private void setDirectory(File directory) {
        this.directory = directory;
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        Method method;
        try {
            method = type.getType().getMethod("create", File.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            method = null;
        }
        if (method == null) {
            return;
        }

        tableModel.setRowCount(0);

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    ITableRowProvider rowProvider = (ITableRowProvider) method.invoke(null, file);
                    if (rowProvider == null) {
                        continue;
                    }
                    Object[] rowData = rowProvider.provideRow();
                    Object[] row = new Object[rowData.length + 1];
                    row[0] = rowProvider;
                    System.arraycopy(rowData, 0, row, 1, rowData.length);
                    tableModel.addRow(row);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (table.getRowCount() > 0) {
            table.getSelectionModel().setSelectionInterval(0, 0);
        }
    }

    private void initializeColumn() {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        tableModel.addColumn(Lang.getString("ui.directory.tab.item"));

        String[] header = type.getHeader();
        for (String column : header) {
            tableModel.addColumn(column);
        }

        int columnId = 1;
        int[] widths = type.getWidth();
        for (int width : widths) {
            table.getColumnModel().getColumn(columnId).setPreferredWidth(width);
            columnId++;
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(180);
    }

    private void refresh() {
        int row = table.getSelectedRow();
        setDirectory(this.directory);
        while (table.getRowCount() <= row) {
            row--;
        }
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    public static DirectoryTabPanel createDirectoryTab(JFrame frame, DirectoryType type, int flags, File directory) {
        DirectoryTabPanel result = new DirectoryTabPanel(frame, type, flags);
        result.setDirectory(directory);
        return result;
    }
}
