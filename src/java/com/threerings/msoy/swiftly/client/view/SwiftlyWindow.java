//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;
import com.threerings.msoy.swiftly.data.PathElement;

/**
 * The base view class for the Swiftly editor. Handles various dialogs and windows.
 */
public interface SwiftlyWindow
{
    /**
     * A callback for when the window is attached to the SwiftlyApplication and displayed.
     */
    public interface AttachCallback
    {
        public void windowDisplayed ();
    }

    /**
     * Shows a modal, internal frame dialog prompting the user to name a {@link PathElement}
     * @param pathElementType the type of {@link PathElement} to name
     * @return the name of the path element. null if the user clicked cancel
     */
    public String showSelectPathElementNameDialog (PathElement.Type pathElementType);

    /**
     * Shows a modal, external frame dialog prompting the user to name a {@link PathElement.FILE}
     * and select the mime type for this file.
     * @param parentElement the PathElement that will be the parent of the returned PathElement
     * @return the new path element. null if the user clicked cancel
     */
    public CreateFileDialog showCreateFileDialog (SwiftlyDocumentEditor editor);

    /**
     * Shows a modal, internal frame dialog asking for user confirmation.
     * Returns true if the user clicked Yes, false if they clicked No.
     */
    public boolean showConfirmDialog (String message);

    /**
     * Show the chat panel.
     */
    public void showChatPanel ();

    /**
     * Hide the chat panel.
     */
    public void hideChatPanel ();
}