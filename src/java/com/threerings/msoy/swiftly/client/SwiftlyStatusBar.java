package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

public class SwiftlyStatusBar extends JPanel {

    public SwiftlyStatusBar (SwiftlyApplet applet) {
        super(new BorderLayout());
        _applet = applet;
        _label = new JLabel();
        add(_label, BorderLayout.CENTER);
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setLabel (String message) {
        _label.setText(message);
    }

    protected SwiftlyApplet _applet;
    protected JLabel _label;
}
