package io.chaofan.chunklauncher;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;

import io.chaofan.chunklauncher.util.Lang;

public class CharSelectDialog extends JDialog {

    private static final long serialVersionUID = -5617610454068651078L;

    public JComboBox chars = new JComboBox();

    public volatile boolean selected = false;

    public CharSelectDialog() {
        this.setResizable(false);
        this.setSize(230, 110);
        this.setLayout(null);

        this.setTitle(Lang.getString("ui.char.title"));

        chars.setLocation(20, 14);
        chars.setSize(190, 23);
        this.add(chars);

        JButton ok = new JButton(Lang.getString("ui.char.ok"));
        ok.setLocation(25, 44);
        ok.setSize(80, 28);
        this.getRootPane().setDefaultButton(ok);
        this.add(ok);

        JButton cancel = new JButton(Lang.getString("ui.char.cancel"));
        cancel.setLocation(125, 44);
        cancel.setSize(80, 28);
        this.add(cancel);

        this.setModal(true);

        ok.addActionListener(e -> {
            selected = true;
            CharSelectDialog.this.setVisible(false);
        });

        cancel.addActionListener(e -> {
            selected = false;
            CharSelectDialog.this.setVisible(false);
        });

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
    }

}
