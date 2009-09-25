//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookInfo;

import client.edutil.FacebookInfoEditorPanel;

public class FacebookAppInfoEditorPanel extends FlowPanel
{
    public FacebookAppInfoEditorPanel (final FacebookInfo info)
    {
        add(new FacebookInfoEditorPanel(info) {
            @Override protected boolean showChromeless () {
                return false;
            }
            @Override protected void saveInfo (FacebookInfo info, AsyncCallback<Void> callback) {
                _appsvc.updateFacebookInfo(info, callback);
            }
            @Override protected String getIntro () {
                return "";
            }
        });
    }

    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
