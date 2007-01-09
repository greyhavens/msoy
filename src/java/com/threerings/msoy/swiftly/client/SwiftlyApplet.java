package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet
{
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

        // XXX TEMP
        initializeProjects();

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

    public AbstractAction createShowProjectDialogAction ()
    {
        return new AbstractAction() {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                showProjectDialog();
            }
        };
    }

    public AbstractAction createNewProjectDialogAction ()
    {
        return new AbstractAction() {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                showNewProjectDialog();
            }
        };
    }

    public SwiftlyEditor getEditor() {
        return _editor;
    }

    public SwiftlyToolbar getToolbar() {
        return _toolbar;
    }

    protected void createGUI ()
    {
        // setup the components
        _editor = new SwiftlyEditor(this);
        _editor.setMinimumSize(new Dimension(400, 0));

        _projectPanel = new SwiftlyProjectPanel(this);
        _projectPanel.setMinimumSize(new Dimension(0, 0));

        _toolbar = new SwiftlyToolbar(this);
        _statusbar = new SwiftlyStatusBar(this);
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _editor, _projectPanel);
        // TODO apparently GTK does not have the graphic for this. What to do?
        _splitPane.setOneTouchExpandable(true);
        _splitPane.setDividerLocation(650);

        // layout the window
        _contentPane.add(_toolbar, BorderLayout.PAGE_START);
        _contentPane.add(_splitPane, BorderLayout.CENTER);
        _contentPane.add(_statusbar, BorderLayout.SOUTH);

        // popup the project selection window
        if (_loadedProject == null) {
            if (getProjects().isEmpty()) {
                showNewProjectDialog();
            } else {
                showProjectDialog();
            }
        } else {
            // TODO this would come in as a parameter to the applet
            loadProject(_loadedProject);
        }
    }

    protected ArrayList<SwiftlyProject> getProjects ()
    {
        // TODO this will need to load the projects from the server
        return _projectList;
    }

    // XXX TEMP
    protected void initializeProjects ()
    {
        ArrayList<SwiftlyDocument> fileList = new ArrayList<SwiftlyDocument>();
        fileList.add(new SwiftlyDocument("file #1", "Example text"));
        fileList.add(new SwiftlyDocument("file #2", "Example text more"));
        SwiftlyProject project = new SwiftlyProject("project1", fileList);
        _projectList.add(project);

        fileList = new ArrayList<SwiftlyDocument>();
        fileList.add(new SwiftlyDocument("file #3", "Example text"));
        fileList.add(new SwiftlyDocument("file #4", "Example text more"));
        project = new SwiftlyProject("project2", fileList);
        _projectList.add(project);
    }

    protected SwiftlyProject createProject (String name)
    {
        // TODO do some business on the server
        ArrayList<SwiftlyDocument> emptyFileList = new ArrayList<SwiftlyDocument>();
        SwiftlyProject project = new SwiftlyProject(name, emptyFileList);
        _projectList.add(project);
        return project;
    }

    protected void loadProject (SwiftlyProject project)
    {
        _loadedProject = project;
        _editor.removeTabs();
        _projectPanel.loadProject(project);
    }

    protected void showProjectDialog ()
    {
        SwiftlyProject project = (SwiftlyProject)JOptionPane.showInternalInputDialog(
            _contentPane, "Select a project:", "Project selection", JOptionPane.QUESTION_MESSAGE,
            null, getProjects().toArray(), null);
        loadProject(project);
    }

    protected void showNewProjectDialog ()
    {
        String projectName = JOptionPane.showInternalInputDialog(
            _contentPane, "Enter the project name", "Create a new project.",
            JOptionPane.INFORMATION_MESSAGE);
        loadProject(createProject(projectName));
    }

    protected Container _contentPane;
    protected SwiftlyEditor _editor;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyStatusBar _statusbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected SwiftlyProject _loadedProject;
    protected JSplitPane _splitPane;
    protected ArrayList<SwiftlyProject> _projectList = new ArrayList<SwiftlyProject>();
}
