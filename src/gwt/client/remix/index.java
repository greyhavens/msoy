//
// $Id$

package client.remix;

import client.shell.Args;
import client.shell.Page;

public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        updateInterface(args);
    }

    protected String getPageId ()
    {
        return "remix";
    }

    protected void updateInterface (Args args)
    {
        if (_createPanel == null) {
            setContent(_createPanel = new DataPackCreationPanel());
        }
    }

    protected DataPackCreationPanel _createPanel;
}
