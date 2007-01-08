package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

public class SwiftlyProjectWindow extends JFrame
{
    public static final String WINDOW_TITLE = "Project selection";

    public SwiftlyProjectWindow (SwiftlyApplet applet, Object[] projectList)
    {
        super(WINDOW_TITLE);
        _applet = applet;
        _list = new JList(projectList);
        _list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add the double click listener
        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    _applet.loadProject((SwiftlyProject)_list.getSelectedValue());
                }
            }
        };
        _list.addMouseListener(mouseListener);

        _label = new JLabel("Choose the project to work on:");
        getContentPane().add(_label, BorderLayout.PAGE_START);
        getContentPane().add(_list, BorderLayout.CENTER);
        pack();
        setVisible(true);
    }

    protected SwiftlyApplet _applet;
    protected JList _list;
    protected JLabel _label;
}
