//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;

/**
 * Module entry point for viewing and editing applications.
 */
public class AppsPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("e")) {
            EditAppPanel panel;
            if (getContent() instanceof EditAppPanel) {
                panel = (EditAppPanel)getContent();
            } else {
                setContent(_msgs.editAppTitle(), panel = new EditAppPanel());
            }
            panel.setApp(args.get(1, 0), args.get(2, 0));

        } else if (action.equals("c")) {
            setContent(new CreateAppPanel());

        } else {
            setContent(_msgs.appsList(), new AppListPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.APPS;
    }

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
}
