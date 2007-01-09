package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet
{
    public SwiftlyEditor editor;

    @Override // from JApplet
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
        // a zero length string makes the label disappear
        _statusbar.setLabel(" ");
    }

    public ShowProjectDialogAction createShowProjectDialogAction ()
    {
        return new ShowProjectDialogAction();
    }


    protected void createGUI ()
    {
        // setup the components
        editor = new SwiftlyEditor(this);
        editor.setMinimumSize(new Dimension(400, 0));

        _projectPanel = new SwiftlyProjectPanel(this);
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
        if (_loadedProject == null) {
            showProjectDialog();
        }
    }

    protected SwiftlyProject[] getProjects ()
    {
        // TODO this will need to load the projects from the server
        ArrayList<SwiftlyDocument> fileList = new ArrayList<SwiftlyDocument>();
        fileList.add(new SwiftlyDocument("file #1", "Example text"));
        fileList.add(new SwiftlyDocument("file #2", "Example text more"));
        SwiftlyProject project = new SwiftlyProject("project1", fileList);

        ArrayList<SwiftlyDocument> fileList2 = new ArrayList<SwiftlyDocument>();
        fileList2.add(new SwiftlyDocument("file #3", "Example text"));
        fileList2.add(new SwiftlyDocument("file #4", "Example text more"));
        SwiftlyProject project2 = new SwiftlyProject("project2", fileList2);

        SwiftlyProject[] projectList = {project, project2};
        return projectList;
    }

    protected void loadProject (SwiftlyProject project)
    {
        _loadedProject = project;
        editor.removeTabs();
        _projectPanel.loadProject(project);
    }

    protected void showProjectDialog ()
    {
        SwiftlyProject project = (SwiftlyProject)JOptionPane.showInternalInputDialog(
            _contentPane, "Select a project:", "Project Selection", JOptionPane.QUESTION_MESSAGE,
            null, getProjects(), null);
        loadProject(project);
    }

    protected class ShowProjectDialogAction extends AbstractAction
    {
        // from AbstractAction
        public void actionPerformed (ActionEvent e) {
            showProjectDialog();
        }
    }

    protected Container _contentPane;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyStatusBar _statusbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected SwiftlyProject _loadedProject;
    protected JSplitPane _splitPane;
}
