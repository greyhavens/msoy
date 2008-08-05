//
// $Id$

package client.help;

import com.google.gwt.core.client.GWT;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;

public class HelpPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new HelpPage();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        setContent(new HelpPanel());
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.HELP;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CHelp.msgs = (HelpMessages)GWT.create(HelpMessages.class);
    }
}
