package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;

public class SwiftlyProjectWindow extends JFrame
{
    public static final String WINDOW_TITLE = "Project selection";

    public SwiftlyProjectWindow (Object[] projectList)
    {
        super(WINDOW_TITLE);
        _list = new JList(projectList);
        _label = new JLabel("Choose the project to work on:");
        getContentPane().add(_label, BorderLayout.PAGE_START);
        getContentPane().add(_list, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    protected JList _list;
    protected JLabel _label;
}
