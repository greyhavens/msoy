package com.threerings.msoy.swiftly.client;        

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

public class SwiftlyEditor extends JPanel
{
    public SwiftlyEditor (SwiftlyProject project)
    {
        super(new BorderLayout());
        _project = project;

        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // TODO the gtk L&F breaks some bits. Just use the default L&F for now.
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }

        // setup the components
        _tabs = new TabbedEditor(this);
        _tabs.setMinimumSize(new Dimension(400, 0));

        _projectPanel = new ProjectPanel(this, project);
        _projectPanel.setMinimumSize(new Dimension(0, 0));

        _toolbar = new EditorToolBar(this);
        _statusbar = new EditorStatusBar();
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _tabs, _projectPanel);
        // TODO apparently GTK does not have the graphic for this. What to do?
        _splitPane.setOneTouchExpandable(true);

        // layout the window
        add(_toolbar, BorderLayout.PAGE_START);
        add(_splitPane, BorderLayout.CENTER);
        add(_statusbar, BorderLayout.SOUTH);

        _splitPane.setDividerLocation(0.8);
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

    public void addEditorTab (SwiftlyDocument document)
    {
        _tabs.addEditorTab(document);
    }

    public void updateTabTitleAt (SwiftlyDocument document)
    {
        _tabs.updateTabTitleAt(document);
    }

    public void updateCurrentTabTitle ()
    {
        _tabs.updateCurrentTabTitle();
    }

    public void closeCurrentTab ()
    {
        _tabs.closeCurrentTab();
    }

    public AbstractAction createCloseCurrentTabAction ()
    {
        return _tabs.createCloseCurrentTabAction();
    }

    public EditorToolBar getToolbar()
    {
        return _toolbar;
    }

    public ProjectPanel getProjectPanel ()
    {
        return _projectPanel;
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

        return JOptionPane.showInternalInputDialog(this, prompt);
    }

    /**
     * Shows a modal, internal frame dialog reporting an error to the user.
     * @param the error message to display
     */
    public void showErrorDialog (String message)
    {
        JOptionPane.showInternalMessageDialog(
            this, message, "An error occurred", JOptionPane.ERROR_MESSAGE);
    }

    protected TabbedEditor _tabs;
    protected EditorToolBar _toolbar;
    protected EditorStatusBar _statusbar;
    protected ProjectPanel _projectPanel;
    protected SwiftlyProject _project;
    protected JSplitPane _splitPane;
}
