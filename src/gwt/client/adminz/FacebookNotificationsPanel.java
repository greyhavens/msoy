//
// $Id$

package client.adminz;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.StringUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.FacebookNotification;
import com.threerings.msoy.admin.gwt.FacebookNotificationStatus;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.PageCallback;

public class FacebookNotificationsPanel extends FlowPanel
{
    public FacebookNotificationsPanel ()
    {
        setStyleName("facebookNotifications");

        add(MsoyUI.createLabel(_msgs.fbNotifsViewTitle(), "Title"));
        add(MsoyUI.createFlowPanel("View",
            _notifs = new SmartTable("View", 0, 1)));

        add(MsoyUI.createLabel(_msgs.fbNotifsStatusTitle(), "Title"));
        add(MsoyUI.createFlowPanel("View",
            _statuses = new SmartTable("View", 0, 1),
            new Button(_msgs.fbNotifsRefresh(), new ClickHandler () {
                public void onClick (ClickEvent event) {
                    refreshStatusList();
                }
            })));

        add(MsoyUI.createLabel(_msgs.fbNotifsEditTitle(), "Title"));
        Button save = new Button(_msgs.fbNotifsSaveBtn());
        new ClickCallback<Void>(save) {
            FacebookNotification notif;
            @Override public boolean callService () {
                if ((notif = createFromForm()) == null) {
                    return false;
                }
                _adminsvc.saveFacebookNotification(notif, this);
                return true;
            }
            @Override public boolean gotResult (Void result) {
                refreshNotifications();
                return true;
            }
        };
        add(MsoyUI.createFlowPanel("Edit",
            makeRow(_msgs.fbNotifsIdLabel(), _id = MsoyUI.createTextBox("manual", 24, 24)),
            makeRow(_msgs.fbNotifsMessageLabel(), _message = MsoyUI.createTextArea("", 50, 6)),
            makeRow("", save)));

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
        };

        refreshNotifications();
        refreshStatusList();
    }

    protected void setNotifs (List<FacebookNotification> notifs)
    {
        while (_notifs.getRowCount() > 0) {
            _notifs.removeRow(_notifs.getRowCount() - 1);
        }

        if (notifs.size() == 0) {
            _notifs.setText(0, 0, _msgs.fbNotifsEmpty());
            return;
        }

        final int ID = 0, TEXT = 1, EDIT_BTN = 2, DELETE_BTN = 3, SEND_BTN = 4;

        int row = 0;
        _notifs.setText(row, ID, _msgs.fbNotifsIdHdr());
        _notifs.setText(row, TEXT, _msgs.fbNotifsTextHdr());
        for (FacebookNotification notif : notifs) {
            _notifs.setText(++row, ID, notif.id, 1);
            _notifs.setText(row, TEXT, StringUtil.truncate(notif.text, 30));

            // edit button
            final FacebookNotification fnotif = notif; 
            _notifs.setWidget(row, EDIT_BTN, new Button(_msgs.fbNotifsEditBtn(),
                new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    _id.setText(fnotif.id);
                    _message.setText(fnotif.text);
                }
            }));

            // delete button
            Button delete = new Button(_msgs.fbNotifsDeleteBtn());
            new ClickCallback<Void>(delete) {
                @Override public boolean callService () {
                    _adminsvc.deleteFacebookNotification(fnotif.id, this);
                    return true;
                }
                @Override public boolean gotResult (Void result) {
                    refreshNotifications();
                    return false;
                }
            };
            _notifs.setWidget(row, DELETE_BTN, delete);

            // send button
            Button send = new Button(_msgs.fbNotifsSendBtn());
            new ClickCallback<Void>(send) {
                @Override public boolean callService () {
                    // TODO: popup for delay input
                    _adminsvc.scheduleFacebookNotification(fnotif.id, 0, this);
                    return true;
                }
                @Override public boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.fbNotifsScheduled());
                    refreshStatusList();
                    return true;
                }
            };
            _notifs.setWidget(row, SEND_BTN, send);
        }
    }

    protected void setStatusList (List<FacebookNotificationStatus> statusList)
    {
        while (_statuses.getRowCount() > 0) {
            _statuses.removeRow(_statuses.getRowCount() - 1);
        }

        if (statusList.size() == 0) {
            _statuses.setText(0, 0, _msgs.fbNotifsStatusEmpty());
            return;
        }

        final int ID = 0, START_TIME = 1, PROGRESS = 2, USER_COUNT = 3, SENT_COUNT = 4;

        int row = 0;
        _statuses.setText(row, ID, _msgs.fbNotifsIdHdr());
        _statuses.setText(row, START_TIME, _msgs.fbNotifsStartedHdr());
        _statuses.setText(row, PROGRESS, _msgs.fbNotifsProgressHdr());
        _statuses.setText(row, USER_COUNT, _msgs.fbNotifsUserCountHdr());
        _statuses.setText(row, SENT_COUNT, _msgs.fbNotifsSentCountHdr());
        for (FacebookNotificationStatus status : statusList) {
            _statuses.setText(++row, ID, status.batchId, 1);
            _statuses.setText(row, PROGRESS, status.progress);
            _statuses.setText(row, START_TIME, fmtDate(status.startTime));
            _statuses.setText(row, USER_COUNT, String.valueOf(status.userCount));
            _statuses.setText(row, SENT_COUNT, String.valueOf(status.sentCount));
        }
    }

    protected FacebookNotification createFromForm ()
    {
        String id = _id.getText().trim();
        if (id.equals("")) {
            MsoyUI.error(_msgs.fbNotifsEmptyIdErr());
            return null;
        }
        String msg = _message.getText().trim();
        if (msg.equals("")) {
            MsoyUI.error(_msgs.fbNotifsEmptyMessageErr());
            return null;
        }
        FacebookNotification text = new FacebookNotification();
        text.id = id;
        text.text = msg;
        return text;
    }

    protected String fmtDate (Date date)
    {
        String value = "-";
        if (date != null) {
            value = DateUtil.formatDateTime(date);
        }
        return value;
    }

    protected Widget makeRow (String label, Widget w)
    {
        return makeRow(label, w, null);
    }

    protected Widget makeRow (String label, Widget w, String suffix)
    {
        FloatPanel row = new FloatPanel("Row");
        row.add(MsoyUI.createLabel(label, null));
        row.add(w);
        if (suffix != null) {
            row.add(MsoyUI.createLabel(suffix, null));
        }
        return row;
    }

    protected void refreshNotifications ()
    {
        _adminsvc.loadFacebookNotifications(new PageCallback<List<FacebookNotification>>(this) {
            @Override public void onSuccess (List<FacebookNotification> result) {
                setNotifs(result);
            }
        });
    }

    protected void refreshStatusList ()
    {
        _adminsvc.loadFacebookNotificationStatus(
            new PageCallback<List<FacebookNotificationStatus>>(this) {
            @Override public void onSuccess (List<FacebookNotificationStatus> result) {
                setStatusList(result);
            }
        });
    }

    protected TextBox _delay, _id;
    protected TextArea _message;
    protected SmartTable _notifs, _statuses;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
