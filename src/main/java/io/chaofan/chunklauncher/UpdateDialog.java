package io.chaofan.chunklauncher;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import io.chaofan.chunklauncher.util.Lang;

public class UpdateDialog extends JDialog {

    private static final long serialVersionUID = 4985542856179003273L;

    private JPanel base = new JPanel();
    private JProgressBar progress = new JProgressBar(0, 1000);
    private JButton cancel = new JButton(Lang.getString("ui.cancel"));

    public UpdateDialog() {
        setResizable(false);
        setModal(false);

        base.setPreferredSize(new Dimension(400, 75));
        base.setLayout(null);

        add(base);
        pack();
        createFrame();
        setTitle(Lang.getString("ui.update.title"));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
    }

    private void createFrame() {
        base.add(cancel);
        cancel.setSize(80, 25);
        cancel.setLocation(160, 40);
        cancel.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        base.add(progress);
        progress.setSize(390, 20);
        progress.setLocation(5, 10);
    }

    public void setProgress(final double d) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progress.setValue((int)((progress.getMaximum() - progress.getMinimum()) * d));
            }
        });
    }

}
