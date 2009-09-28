//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.apps.gwt.AppInfo;

import client.edutil.EditorTable;
import client.edutil.EditorUtil;
import client.ui.MsoyUI;

/**
 * Common classes and methods related to editing and viewing applications.
 */
public class AppUtil
{
    /**
     * Adds a text box row to the given editor that will bind to the name field of the given info
     * on save, checking the length.
     */
    public static void addNameBox (EditorTable editor, final AppInfo info)
    {
        final int maxNameLen = AppInfo.MAX_NAME_LENGTH;
        final TextBox name = MsoyUI.createTextBox(info.name, maxNameLen, maxNameLen);
        editor.addRow(_msgs.editAppInfoName(), name, new Command() {
            public void execute () {
                info.name = EditorUtil.checkName(name.getText().trim(), maxNameLen);
            }
        });
    }

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
}
