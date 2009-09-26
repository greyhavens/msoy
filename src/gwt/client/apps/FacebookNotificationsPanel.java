//
// $Id$

package client.apps;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.apps.gwt.FacebookNotification;

import client.edutil.EditorTable;
import client.edutil.EditorUtil.ConfigException;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

public class FacebookNotificationsPanel extends FlowPanel
{
    public FacebookNotificationsPanel (AppInfo info)
    {
        addStyleName("facebookNotifications");
        _appId = info.appId;

        // view
        add(MsoyUI.createHTML(_msgs.fbNotifsTitle(info.name), "Title"));
        final NotificationsList notifs = new NotificationsList();
        add(notifs);

        // edit or create a notification
        final FacebookNotification editing = new FacebookNotification();
        final EditorTable editor = new EditorTable();
        add(editor);
        editor.addWidget(MsoyUI.createHTML(_msgs.fbNotifsEditTitle(), "Title"), 2);
        editor.addRow(_msgs.fbNotifsIdLabel(), _id = MsoyUI.createTextBox("", 24, 24),
            new Command() {
            @Override public void execute () {
                editing.id = _id.getText().trim();
                if (editing.id.equals("")) {
                    throw new ConfigException(_msgs.fbNotifsEmptyIdErr());
                }
            }
        });
        editor.addRow(_msgs.fbNotifsTextLabel(), _text = MsoyUI.createTextArea("", 50, 6),
            new Command() {
            @Override public void execute () {
                editing.text = _text.getText().trim();
                if (editing.text.equals("")) {
                    throw new ConfigException(_msgs.fbNotifsEmptyMessageErr());
                }
            }
        });

        Button save = editor.addSaveRow();
        new ClickCallback<Void>(save) {
            @Override public boolean callService () {
                if (editor.bindChanges()) {
                    _appsvc.saveNotification(_appId, editing, this);
                }
                return true;
            }
            @Override public boolean gotResult (Void result) {
                notifs.refresh();
                return true;
            }
        };

        /* TODO
        add(MsoyUI.createLabel(_msgs.fbNotifsDailyTitle(), "Title"));
        final TextArea dailyNotifications = MsoyUI.createTextArea("", 32, 5);
        Button updateRotation = new Button(_msgs.fbNotifsDailyUpdateBtn());
        add(MsoyUI.createFlowPanel("Rotation",
            makeRow(_msgs.fbNotifsDailyLabel(), dailyNotifications),
            makeRow("", updateRotation)));

        new ClickCallback<Void>(updateRotation) {
            @Override public boolean callService () {
                String[] ids = dailyNotifications.getText().split("( |\t|\r|\n)+");
                _adminsvc.setDailyNotifications(ids, this);
                return false;
            }
            @Override public boolean gotResult (Void result) {
                MsoyUI.info(_msgs.fbNotifsDailyUpdated());
                return true;
            }
        };*/
    }

    protected class NotificationsList extends SmartTable
    {
        public NotificationsList ()
        {
            super("notificationsList", 5, 0);
            setWidget(0, 0, MsoyUI.createNowLoading());
            refresh();
        }

        public void refresh ()
        {
            _appsvc.loadNotifications(_appId,
                new InfoCallback<List<FacebookNotification>> () {
                    @Override public void onSuccess (List<FacebookNotification> result) {
                        setNotifications(result);
                    }
                });
        }

        public void setNotifications (List<FacebookNotification> notifs)
        {
            while (getRowCount() > 0) {
                removeRow(getRowCount() - 1);
            }
    
            if (notifs.size() == 0) {
                setText(0, 0, _msgs.fbNotifsEmpty());
                return;
            }
    
            final int ID = 0, TEXT = 1, EDIT_BTN = 2, DELETE_BTN = 3, SEND_BTN = 4;
    
            int row = 0;
            setText(row, ID, _msgs.fbNotifsIdHdr(), 1, "Header", "Id");
            setText(row, TEXT, _msgs.fbNotifsTextHdr(), 1, "Header", "Text");
            getRowFormatter().setStyleName(row++, "Row");

            for (FacebookNotification notif : notifs) {
                setText(row, ID, notif.id, 1, "Id");
                setText(row, TEXT, StringUtil.truncate(notif.text, 50), 1, "Text");
    
                // edit button
                final FacebookNotification fnotif = notif; 
                setWidget(row, EDIT_BTN, new Button(_msgs.fbNotifsEditBtn(),
                    new ClickHandler() {
                    @Override public void onClick (ClickEvent event) {
                        _id.setText(fnotif.id);
                        _text.setText(fnotif.text);
                    }
                }));
    
                // delete button
                Button delete = new Button(_msgs.fbNotifsDeleteBtn());
                new ClickCallback<Void>(delete) {
                    @Override public boolean callService () {
                        _appsvc.deleteNotification(_appId, fnotif.id, this);
                        return true;
                    }
                    @Override public boolean gotResult (Void result) {
                        refresh();
                        return false;
                    }
                };
                setWidget(row, DELETE_BTN, delete);
    
                // send button
                Button send = new Button(_msgs.fbNotifsSendBtn());
                new ClickCallback<Void>(send) {
                    @Override public boolean callService () {
                        // TODO: popup for delay input
                        _appsvc.scheduleNotification(_appId, fnotif.id, 0, this);
                        return true;
                    }
                    @Override public boolean gotResult (Void result) {
                        MsoyUI.info(_msgs.fbNotifsScheduled());
                        refresh();
                        return true;
                    }
                };
                setWidget(row, SEND_BTN, send);

                getRowFormatter().setStyleName(row, "Row");
                if (row % 2 == 1) {
                    getRowFormatter().addStyleName(row, "AltRow");
                }
                row++;
            }
        }
    }

    protected int _appId;
    protected TextBox _id;
    protected TextArea _text;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
