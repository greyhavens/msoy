package com.threerings.msoy.swiftly.client;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class SwiftlyImagePane extends JPanel
    implements PositionableComponent
{
    public SwiftlyImagePane (SwiftlyContext ctx, SwiftlyImageDocument document)
    {
        _ctx = ctx;
        _document = document;

        add(_label = new JLabel());

        displayImage();
    }

    public void displayImage ()
    {
        _label.setIcon(new ImageIcon(_document.getImage()));
    }

    // from PositionableComponent
    public Component getComponent ()
    {
        return this;
    }

    // from PositionableComponent
    public void gotoLocation (int row, int column, boolean highlight)
    {
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyImageDocument _document;

    protected JLabel _label;
}
