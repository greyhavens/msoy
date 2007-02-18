//
// $Id$

package client.group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

import com.threerings.msoy.web.data.WebCreds;

import client.msgs.MsgsEntryPoint;
import client.shell.MsoyEntryPoint;

public class index extends MsgsEntryPoint
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

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        // "list" is used as a token to get to the GroupList, because you can't give GWT an
        // empty token string for either a Hyperlink or History.newItem()
        if (token.length() == 0 || token.equals("list")) {
            setContent(new GroupList());
        } else if (token.startsWith("tag=")) {
            setContent(new GroupList(token.substring(4)));
        } else {
            setContent(new GroupView(Integer.parseInt(token)));
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "group";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CGroup.msgs = (GroupMessages)GWT.create(GroupMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        onHistoryChanged(History.getToken());
        return header;
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        onHistoryChanged(History.getToken());
    }
}
