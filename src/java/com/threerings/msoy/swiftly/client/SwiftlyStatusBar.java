package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.tree.DefaultMutableTreeNode;

public class SwiftlyStatusBar extends JPanel {

    public SwiftlyStatusBar() {
        super(new BorderLayout());
        _label = new JLabel();
        add(_label, BorderLayout.CENTER);
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setLabel(String message) {
        _label.setText(message);
    }

    protected JLabel _label;
}
