package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

public class EditorStatusBar extends JPanel
{
    public EditorStatusBar ()
    {
        super(new BorderLayout());
        _label = new JLabel();
        add(_label, BorderLayout.CENTER);
        setBorder(new BevelBorder(BevelBorder.LOWERED));
    }

    public void setLabel (String message)
    {
        _label.setText(message);
    }

    protected JLabel _label;
}
