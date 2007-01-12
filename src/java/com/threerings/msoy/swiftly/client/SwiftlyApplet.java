package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
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

        // Load the authentication token and configure the XML-RPC connection
        String authtoken = getParameter("authtoken");

        // Configure RPC
        URL rpcURL;
        try {
            rpcURL = new URL(getParameter("rpcURL"));
        } catch (MalformedURLException e) {
            // TODO: Useful error handling
            System.err.println("GWT Application provided a bum URL");
            return;
        }
        _swiftlyRpc = new SwiftlyClientRpc(rpcURL, authtoken);

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete.");
            Exception ee = (Exception) e.getCause();
            ee.printStackTrace();
            System.out.println("Foo: " + ee);
        }

        // if we managed to load a project, show the gui, otherwise show an error
        if (_loadedProject != null) {
            showComponents(true);
            // TODO this actually seems to work at random intervals. Fix.
            _splitPane.setDividerLocation(0.8);

            setStatus("Welcome to Swiftly!");
        } else {
            showErrorDialog("No project found to load.");
        }
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
        return new AbstractAction("Switch Project") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                showProjectSelectionDialog();
            }
        };
    }

    public AbstractAction createNewProjectDialogAction ()
    {
        return new AbstractAction("Create Project") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                showCreateProjectDialog();
            }
        };
    }

    /**
     * Shows a modal, internal frame dialog prompting the user to name a {@link FileElement}
     * @param the type of {@link FileElement} to name
     * @return true if the user picked a name, false if they clicked cancel
     */
    public String showSelectFileElementNameDialog (int fileElementType)
    {
        String prompt;
        switch (fileElementType) {
        case FileElement.PROJECT:
            prompt = "Enter the project name: ";
            break;
        case FileElement.DIRECTORY:
            prompt = "Enter the directory name: ";
            break;
        case FileElement.DOCUMENT:
            prompt = "Enter the file name: ";
            break;
        default:
            prompt = "Enter the name: ";
        }

        return JOptionPane.showInternalInputDialog(_contentPane, prompt);
    }

    /**
     * Shows a modal, internal frame dialog reporting an error to the user.
     * @param the error message to display
     */
    public void showErrorDialog (String message)
    {
        JOptionPane.showInternalMessageDialog(
            _contentPane, message, "An error occurred", JOptionPane.ERROR_MESSAGE);
    }

    public SwiftlyEditor getEditor()
    {
        return _editor;
    }

    public SwiftlyToolbar getToolbar()
    {
        return _toolbar;
    }

    public SwiftlyProjectPanel getProjectPanel ()
    {
        return _projectPanel;
    }

    /**
     * Saves a file element on the backend. Creates the element if it doesn't already exist. 
     * @param element the {@link FileElement} to save.
     */
    public void saveFileElement (FileElement element)
    {
        // TODO save the file element on the backend
        // TODO show a progress bar in the status bar while Saving...
    }

    /**
     * Deletes a file element on the backend.
     * @param element the {@link FileElement} to delete.
     */
    public void deleteFileElement (FileElement element)
    {
        // TODO delete the file element on the backend
    }

    /**
     * Renames a file element on the backend.
     * @param element the {@link FileElement} to rename.
     */
    public void renameFileElement (FileElement element, String newName)
    {
        // TODO rename the element on the backend
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

        // hides the components initially
        showComponents(false);

        // layout the window
        _contentPane.add(_toolbar, BorderLayout.PAGE_START);
        _contentPane.add(_splitPane, BorderLayout.CENTER);
        _contentPane.add(_statusbar, BorderLayout.SOUTH);

        if (_loadedProject != null) {
            // TODO this would come in as a parameter to the applet
            loadProject(_loadedProject);
        } else {
            if (getProjects().isEmpty()) {
                // if we have no projects on the server, pop up the create project dialog
                showCreateProjectDialog();
            } else {
                // popup the project selection window
                showProjectSelectionDialog();
            }
        }
    }

    protected void showComponents(boolean value)
    {
        _toolbar.setVisible(value);
        _splitPane.setVisible(value);
        _statusbar.setVisible(value);
    }

    protected ArrayList<SwiftlyProject> getProjects ()
    {
        // Fetch the list of projects from the server
        return _swiftlyRpc.getProjects();
    }

    protected void showCreateProjectDialog ()
    {
        // prompt the user for the project name
        String name = showSelectFileElementNameDialog(FileElement.PROJECT);
        // if the user hit cancel do no more
        if (name == null) {
            return;
        }
        createProject(name);
    }

    protected void createProject (String name)
    {
        // TODO this needs to be in a try/catch block
        loadProject(_swiftlyRpc.createProject(name));
    }

    protected void loadProject (SwiftlyProject project)
    {
        _loadedProject = project;
        _editor.removeTabs();
        _projectPanel.loadProject(project);
    }

    protected void showProjectSelectionDialog ()
    {
        SwiftlyProject project = (SwiftlyProject)JOptionPane.showInternalInputDialog(
            _contentPane, "Select a project:", "Project selection", JOptionPane.QUESTION_MESSAGE,
            null, getProjects().toArray(), null);

        // if the user hit cancel do no more
        if (project == null) {
            return;
        }
        loadProject(project);
    }

    protected Container _contentPane;
    protected SwiftlyEditor _editor;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyStatusBar _statusbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected SwiftlyProject _loadedProject;
    protected JSplitPane _splitPane;

    /** Swiftly RPC Connection. */
    SwiftlyClientRpc _swiftlyRpc;
}
