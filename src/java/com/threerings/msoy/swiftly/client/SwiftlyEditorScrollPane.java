package com.threerings.msoy.swiftly.client;

import javax.swing.JScrollPane;

public class SwiftlyEditorScrollPane extends JScrollPane
{
    public SwiftlyEditorScrollPane(SwiftlyTextPane pane)
    {
        super(pane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        _textPane = pane;
    }
    
    public SwiftlyTextPane getTextPane ()
    {
        return _textPane; 
    }       

    protected SwiftlyTextPane _textPane;
}

