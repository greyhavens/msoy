package com.threerings.msoy.swiftly.client;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet
{
    @Override // from JApplet
    public void init()
    {
        // Load the authentication token and configure the XML-RPC connection
        String authtoken = getParameter("authtoken");

        String roomId = getParameter("roomId");

        // TODO load the project using the roomID

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI(new SwiftlyProject("Coolest game ever"));
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete.");
            Exception ee = (Exception) e.getCause();
            ee.printStackTrace();
            System.out.println("Foo: " + ee);
        }
    }

    public SwiftlyEditor getEditor()
    {
        return _editor;
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

    protected void createGUI (SwiftlyProject project)
    {
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // TODO the gtk L&F breaks some bits. Just use the default L&F for now.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }

        _editor = new SwiftlyEditor(project);
        setContentPane(_editor);
    }

    protected SwiftlyEditor _editor;
}
