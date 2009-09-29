//
// $Id$

package client.edutil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.threerings.gwt.util.StringUtil;
import com.threerings.msoy.facebook.gwt.FacebookInfo;

import client.edutil.EditorTable;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Abstract class for editing Facebook info. Subclassed for apps and games.
 */
public abstract class FacebookInfoEditorPanel extends EditorTable
{
    public FacebookInfoEditorPanel (final FacebookInfo info)
    {
        addStyleName("facebookInfo");
        addWidget(MsoyUI.createHTML(getIntro(), null), 2);
        addSpacer();
        _viewRow = addRow("", MsoyUI.createHTML("", null), null);
        updateAppLink(info);

        final TextBox key = MsoyUI.createTextBox(
            info.apiKey, FacebookInfo.KEY_LENGTH, FacebookInfo.KEY_LENGTH);
        addRow(_msgs.infoEditKey(), key, new Command() {
            public void execute () {
                info.apiKey = key.getText().trim();
            }
        });

        final TextBox secret = MsoyUI.createTextBox(
            info.appSecret, FacebookInfo.SECRET_LENGTH, FacebookInfo.SECRET_LENGTH);
        addRow(_msgs.infoEditSecret(), secret, new Command() {
            public void execute () {
                info.appSecret = secret.getText().trim();
            }
        });

        final TextBox fbUid = MsoyUI.createTextBox(String.valueOf(info.fbUid), 20, 20);
        addRow(_msgs.infoEditFbUid(), fbUid, new Command() {
            public void execute () {
                try {
                    info.fbUid = Long.valueOf(fbUid.getText().trim());
                } catch (NumberFormatException nfe) {
                    info.fbUid = 0;
                }
            }
        });

        final TextBox canvasName = MsoyUI.createTextBox(
            info.canvasName, FacebookInfo.CANVAS_NAME_LENGTH, 20);
        addRow(_msgs.infoEditCanvasName(), canvasName, new Command() {
            public void execute () {
                info.canvasName = canvasName.getText().trim();
            }
        });

        if (showChromeless()) {
            final CheckBox chromeless = new CheckBox(_msgs.infoEditChromelessText());
            chromeless.setValue(info.chromeless);
            addRow(_msgs.infoEditChromeless(), chromeless, new Command() {
                public void execute () {
                    info.chromeless = chromeless.getValue();
                }
            });
        }

        addSpacer();

        Button save = addSaveRow();
        new ClickCallback<Void>(save) {
            protected boolean callService () {
                if (!bindChanges()) {
                    return false;
                }
                saveInfo(info, this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.infoEditUpdated());
                updateAppLink(info);
                return true;
            }
        };

    }

    /**
     * Gets the text to show at the top of the panel explaining what this is all about.
     */
    abstract protected String getIntro ();

    /**
     * Returns true if this instance should display the chromeless tick box.
     */
    abstract protected boolean showChromeless();

    /**
     * Invokes the service call to save the facebook info.
     */
    abstract protected void saveInfo(FacebookInfo info, AsyncCallback<Void> callback);

    protected void updateAppLink (FacebookInfo info)
    {
        setWidget(_viewRow, 1, MsoyUI.createHTML(_msgs.infoEditViewApp(info.apiKey), null));
        getRowFormatter().setVisible(_viewRow, !StringUtil.isBlank(info.apiKey));
    }

    protected int _viewRow;
    protected static final EditorUtilMessages _msgs = GWT.create(EditorUtilMessages.class);
}
