package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JApplet;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet
{
    public SwiftlyEditor editor;

    public void init()
    {
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // TODO the gtk L&F breaks some bits. Just use the default L&F for now.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }

        // Save the root content pane
        _contentPane = getContentPane();

        // TODO do we want this?
        // setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete");
        }

        setStatus("Welcome to Swiftly!");
    }

    public void setStatus (String msg)
    {
        _statusbar.setLabel(msg);
    }

    public void clearStatus ()
    {
        // a null string makes the label disappear
        _statusbar.setLabel(" ");
    }

    protected void createGUI ()
    {
        // setup the components
        editor = new SwiftlyEditor(this);
        editor.setMinimumSize(new Dimension(400, 0));

        ArrayList<SwiftlyDocument> fileList = new ArrayList<SwiftlyDocument>();
        fileList.add(new SwiftlyDocument("file #1", "Example text"));
        fileList.add(new SwiftlyDocument("file #2", "Example text more"));
        _projectPanel = new SwiftlyProjectPanel(this, "My cool project", fileList);
        _projectPanel.setMinimumSize(new Dimension(0, 0));

        _toolbar = new SwiftlyToolbar(this);
        _statusbar = new SwiftlyStatusBar(this);
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor, _projectPanel);
        // TODO apparently GTK does not have the graphic for this. What to do?
        _splitPane.setOneTouchExpandable(true);
        _splitPane.setDividerLocation(650);

        // layout the window
        _contentPane.add(_toolbar, BorderLayout.PAGE_START);
        _contentPane.add(_splitPane, BorderLayout.CENTER);
        _contentPane.add(_statusbar, BorderLayout.SOUTH);

        // popup the project selection window
        // TODO don't popup if we already know our project
        // XXX TEMP disable while I work on other bits
        /*
        String[] projectList = {"project1", "project2", "project3"};
        _projectWindow = new SwiftlyProjectWindow(projectList);
        */
    }

    protected Container _contentPane;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyStatusBar _statusbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected SwiftlyProjectWindow _projectWindow;
    protected JSplitPane _splitPane;
} 
