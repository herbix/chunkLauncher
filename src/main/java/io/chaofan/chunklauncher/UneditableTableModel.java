package io.chaofan.chunklauncher;

import javax.swing.table.DefaultTableModel;

public class UneditableTableModel extends DefaultTableModel {

    private static final long serialVersionUID = -2140422989547704774L;

    @Override
    public boolean isCellEditable(int col, int row) {
        return false;
    }
}
