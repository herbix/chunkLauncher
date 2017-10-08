package io.chaofan.chunklauncher.util;

import java.awt.*;

/**
 * Created by Chaofan on 2017/10/8.
 */
public class UI {

    public static GridBagConstraints gbc(int gridx, int gridy) {
        GridBagConstraints result = new GridBagConstraints();
        result.gridx = gridx;
        result.gridy = gridy;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, double weightx, double weighty) {
        GridBagConstraints result = gbc(gridx, gridy);
        result.weightx = weightx;
        result.weighty = weighty;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, double weightx, double weighty, int fill) {
        GridBagConstraints result = gbc(gridx, gridy, weightx, weighty);
        result.fill = fill;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, int width, int height, double weightx, double weighty, int fill) {
        GridBagConstraints result = gbc(gridx, gridy, weightx, weighty, fill);
        result.gridwidth = width;
        result.gridheight = height;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, Insets insets) {
        GridBagConstraints result = gbc(gridx, gridy);
        result.insets = insets;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, double weightx, double weighty, int fill, Insets insets) {
        GridBagConstraints result = gbc(gridx, gridy, weightx, weighty, fill);
        result.insets = insets;
        return result;
    }

    public static GridBagConstraints gbc(int gridx, int gridy, int width, int height, double weightx, double weighty, int fill, Insets insets) {
        GridBagConstraints result = gbc(gridx, gridy, weightx, weighty, fill, insets);
        result.gridwidth = width;
        result.gridheight = height;
        return result;
    }

    public static Insets insets(int v) {
        return new Insets(v, v, v, v);
    }

    public static Insets insets(int y, int x) {
        return new Insets(y, x, y, x);
    }

    public static Insets insets(int t, int l, int b, int r) {
        return new Insets(t, l, b, r);
    }
}
