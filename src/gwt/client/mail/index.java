//
// $Id$

package client.mail;

import client.MsoyEntryPoint;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.threerings.msoy.web.data.MailFolder;

public class index extends MsoyEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // @Override // from MsoyEntryPoint
    public void onPageLoad ()
    {
        _mainView = new MailApplication(_ctx);
        setContent(_mainView);
        History.addHistoryListener(this);
        onHistoryChanged(History.getToken());
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        int folderId = MailFolder.INBOX_FOLDER_ID;
        int messageId = -1;
        
        if (token.length() > 0) {
            try {
                String[] bits = token.substring(1).split(":");
                folderId = Integer.parseInt(bits[0]);
                if (bits.length == 2) {
                    messageId = Integer.parseInt(bits[1]);
                }
            } catch (Exception e) {
                // just use the defaults
            }
        }
        _mainView.showFolder(folderId);
        _mainView.showMessage(messageId);
    }
    protected MailApplication _mainView;
}
