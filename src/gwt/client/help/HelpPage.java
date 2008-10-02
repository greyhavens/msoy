//
// $Id$

package client.help;

import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;

public class HelpPage extends Page
{
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
}
