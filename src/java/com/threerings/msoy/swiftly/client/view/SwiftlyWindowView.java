//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.micasa.client.ChatPanel;
import com.threerings.micasa.client.OccupantList;
import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Implementation of SwiftlyWindow.
 */
public class SwiftlyWindowView extends JPanel
    implements SwiftlyWindow
{
    public SwiftlyWindowView (ProjectPanelView projectPanel, EditorToolBarView toolbar,
                              TabbedEditorView editorTabs, CrowdContext ctx, Translator translator,
                              AttachCallback callback)
    {
        _translator = translator;
        _callback = callback;

        setLayout(new VGroupLayout(
                      GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.TOP));
        // let's not jam ourselves up against the edges of the window
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add our toolbar
        add(toolbar, GroupLayout.FIXED);

        // set up the left pane: the tabbed editor
        editorTabs.setMinimumSize(new Dimension(400, 400));

        // set up the right pane: project panel and chat
        _projectPanel = projectPanel;
        _projectPanel.setMinimumSize(new Dimension(200, 200));

        JPanel chatPanel = new JPanel(
            new HGroupLayout(GroupLayout.STRETCH, GroupLayout.STRETCH, 5, GroupLayout.LEFT));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OccupantList ol;
        // TODO: these two panels require the context.. can we work around this?
        chatPanel.add(ol = new OccupantList(ctx), GroupLayout.FIXED);
        ol.setPreferredSize(new Dimension(50, 0));
        chatPanel.add(new ChatPanel(ctx, false));
        chatPanel.setMinimumSize(new Dimension(0, 0));
        chatPanel.setPreferredSize(new Dimension(200, 200));

        _rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _projectPanel, chatPanel);
        _rightPane.setOneTouchExpandable(true);
        // give the top pane any extra space
        _rightPane.setResizeWeight(1);

        _contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorTabs, _rightPane);
        _contentPane.setOneTouchExpandable(true);
        // give the left pane any extra space
        _contentPane.setResizeWeight(1);
        add(_contentPane);
    }

    @Override // from Component
    public void doLayout ()
    {
        super.doLayout();

        // set up our divider location and trigger the callback when we are first displayed
        if (_firstLayout && getHeight() > 0) {
            _firstLayout = false;
            _contentPane.resetToPreferredSizes();
            _callback.windowDisplayed();
        }
    }

    // from SwiftlyWindow
    // TODO: this is only being used to name directories. Consider simplifying
    public String showSelectPathElementNameDialog (PathElement.Type pathElementType)
    {
        return JOptionPane.showInternalInputDialog(
            this, _translator.xlate("m.dialog.select_name." + pathElementType));
    }

    // from SwiftlyWindow
    public CreateFileDialog showCreateFileDialog (SwiftlyDocumentEditor editor)
    {
        return new CreateFileDialog(editor, _translator, _projectPanel);
    }

    // from SwiftlyWindow
    public boolean showConfirmDialog (String message)
    {
        int response = JOptionPane.showInternalConfirmDialog(
            this, message, _translator.xlate("m.dialog.confirm.title"), JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    // from SwiftlyWindow
    public void showChatPanel ()
    {
        // only show the chat panel if the split pane has hidden it completely
        if (_rightPane.getDividerLocation() == _rightPane.getMaximumDividerLocation()) {
            _rightPane.resetToPreferredSizes();
        }
    }

    // from SwiftlyWindow
    public void hideChatPanel ()
    {
        // this is a bit of a hack, but a better way has not presented itself
        _rightPane.setDividerLocation(getHeight());
    }

    /** A flag to indicate the first time this component has been laid out. */
    private boolean _firstLayout = true;

    private final Translator _translator;
    private final AttachCallback _callback;

    private final JSplitPane _contentPane;
    private final JSplitPane _rightPane;
    private final ProjectPanelView _projectPanel;
}
