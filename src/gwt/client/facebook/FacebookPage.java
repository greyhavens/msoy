//
// $Id$

package client.facebook;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.shell.Page;

/**
 * Displays Facebook stuff like a list of friends who have played whirled and some status.
 */
public class FacebookPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("")) {
            ServerFBMLPanel fbml = new ServerFBMLPanel();
            fbml.add(MsoyUI.createLabel("Table with nested fb:name:", null));
            SmartTable test = new SmartTable(10, 10);
            test.setWidget(0, 0, new FBMLPanel("name", "uid", "507435487")); // Shanti
            test.setWidget(0, 1, new FBMLPanel("name", "uid", "540615819")); // Michael
            test.setWidget(1, 0, new FBMLPanel("name", "uid", "553328385")); // Jamie
            test.setWidget(1, 1, new FBMLPanel("name", "uid", "532587813")); // Daniel
            fbml.add(test);
            setContent("FBML Test", fbml);
            fbml.reparse();

        } else if (action.equals("invite")) {
            
        }
    }

    @Override // from Page
    public Pages getPageId ()
    {
        return Pages.FACEBOOK;
    }

    @Override // from Page
    protected boolean isTitlePage ()
    {
        return false;
    }
}
