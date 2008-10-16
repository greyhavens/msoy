//
// $Id$

package client.help;

import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.shell.Page;

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
