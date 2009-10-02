//
// $Id$

package client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.apps.gwt.AppService.AppData;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.NaviTabPanel;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Panel for editing all of the data associated with an application.
 */
public class EditAppPanel extends FlowPanel
{
    /**
     * Creates a new application editor.
     */
    public EditAppPanel ()
    {
        setStyleName("editApp");
        add(MsoyUI.createNowLoading());
    }

    /**
     * Sets the app being edited to the one with the given id and selects the given tab.
     */
    public void setApp (int appId, final int tab)
    {
        if (appId == _appId) {
            _tabs.activateTab(tab);
            return;
        }

        _appId = appId;
        _appsvc.getAppData(appId, new InfoCallback<AppData>() {
            public void onSuccess (AppData data) {
                setAppData(data);
                _tabs.activateTab(tab);
            }
        });
    }

    protected void setAppData (final AppData data)
    {
        clear();

        SmartTable header = new SmartTable("Header", 0, 10);
        header.setText(0, 0, data.info.name, 1, "Title");
        //header.setWidget(0, 1, MsoyUI.createHTML(_msgs.egTip(), null), 1, "Tip");
        Button delete = new Button(_msgs.editAppsDeleteBtn());
        header.setWidget(0, 2, delete);
        header.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        add(header);

        // wire up the delete button
        new ClickCallback<Void>(delete, _msgs.editAppsDeleteConfirm()) {
            @Override protected boolean callService () {
                _appsvc.deleteApp(data.info.appId, this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                Link.go(Pages.APPS);
                return true;
            }
        };

        // add our tab list of doom
        add(_tabs = new NaviTabPanel(Pages.APPS) {
            protected Args getTabArgs (int tabIdx) {
                return Args.compose("e", _appId, tabIdx);
            }
        });

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new AppInfoEditorPanel(data);
            }
        }, _msgs.editAppTabInfo());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FacebookAppInfoEditorPanel(data.facebook);
            }
        }, _msgs.editAppTabFacebook());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FacebookNotificationsPanel(data.info);
            }
        }, _msgs.editAppTabNotifications());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FacebookNotificationStatusPanel(data.info);
            }
        }, _msgs.editAppTabNotificationStatus());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FacebookTemplatesPanel(data.info);
            }
        }, _msgs.editAppTabTemplates());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FeedThumbnailsPanel(data.info);
            }
        }, _msgs.editAppTabFeedThumbnails());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new KontagentInfoPanel(data.info, data.kontagent);
            }
        }, _msgs.editAppTabKontagentInfo());

        _tabs.add(new LazyPanel() {
            protected Widget createWidget () {
                return new FacebookDailyNotificationsPanel(data.info, data.dailyNotifications);
            }
        }, _msgs.editAppTabDailyNotifs());
    }

    protected int _appId;
    protected NaviTabPanel _tabs;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
