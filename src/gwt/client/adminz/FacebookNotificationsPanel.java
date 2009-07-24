//
// $Id$

package client.adminz;

import java.util.Date;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.admin.gwt.FacebookNotification;
import com.threerings.msoy.facebook.gwt.NotificationStatus;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.StringUtil;

public class FacebookNotificationsPanel extends AdminDataPanel<List<FacebookNotification>>
{
    public FacebookNotificationsPanel ()
    {
        super("facebookNotifications");
        _adminsvc.loadFacebookNotifications(createCallback());
    }

    @Override // from DataPanel
    public void init (List<FacebookNotification> result)
    {
        add(MsoyUI.createLabel(_msgs.fbNotifsSavedTitle(), "Title"));
        add(MsoyUI.createFlowPanel("Saved", _saved = new SmartTable("Saved", 5, 5)));
        _saved.setText(0, 0, _msgs.fbNotifsIdHdr());
        _saved.setText(0, 1, _msgs.fbNotifsTextHdr());
        for (FacebookNotification notif : result) {
            updateNotification(notif);
        }
        add(MsoyUI.createLabel(_msgs.fbNotifsSetupTitle(), "Title"));
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
                updateNotification(notif);
                return true;
            }
        };
        add(MsoyUI.createFlowPanel("Edit",
            makeRow(_msgs.fbNotifsIdLabel(), _id = MsoyUI.createTextBox("manual", 24, 24)),
            makeRow(_msgs.fbNotifsMessageLabel(), _message = MsoyUI.createTextArea("", 50, 6)),
            makeRow(_msgs.fbNotifsDelayLabel(), _delay = MsoyUI.createTextBox("1", 3, 3),
                _msgs.fbNotifsDelayUnits()),
            makeRow("", new Button(_msgs.fbNotifsScheduleBtn(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    send();
                }
            })),
            makeRow("", save)));
        add(MsoyUI.createLabel(_msgs.fbNotifsStatusTitle(), "Title"));
        add(MsoyUI.createFlowPanel("Status",
            new Button(_msgs.fbNotifsRefresh(), new ClickHandler () {
                public void onClick (ClickEvent event) {
                    _adminsvc.getFacebookNotificationStatuses(
                        new InfoCallback<List<NotificationStatus>>() {
                        public void onSuccess (List<NotificationStatus> result) {
                            setStatuses(result);
                        }
                    });
                }
            }),
            _status = new SmartTable("Status", 5, 5)));
    }

    protected int findRow (FacebookNotification notif)
    {
        for (int find = 1; find < _saved.getRowCount(); ++find) {
            if (_saved.getText(find, 0).equals(notif.id)) {
                return find;
            }
        }
        return -1;
    }

    protected void updateNotification (final FacebookNotification notif)
    {
        int row = findRow(notif);
        if (row == -1) {
            row = _saved.addText(notif.id, 1);
        }
        _saved.setText(row, 1, StringUtil.truncate(notif.text, 50));
        _saved.setWidget(row, 2, new Button(_msgs.fbNotifsEditBtn(), new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                _id.setText(notif.id);
                _message.setText(notif.text);
            }
        }));
        Button delete = new Button(_msgs.fbNotifsDeleteBtn());
        new ClickCallback<Void> (delete) {
            @Override public boolean callService () {
                _adminsvc.deleteFacebookNotification(notif.id, this);
                return true;
            }
            @Override public boolean gotResult (Void result) {
                _saved.removeRow(findRow(notif));
                return true;
            }
        };
        _saved.setWidget(row, 3, delete);
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

    protected void send ()
    {
        FacebookNotification text = createFromForm();
        if (text == null) {
            return;
        }
        int delay;
        try {
            delay = Integer.parseInt(_delay.getText());
        } catch (Exception e) {
            MsoyUI.error(_msgs.fbNotifsDelayFmtErr());
            return;
        }
        _adminsvc.sendFacebookNotification(text.id, text.text, delay, new InfoCallback<Void> () {
            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.fbNotifsScheduled());
            }
        });
    }

    protected void setStatuses (List<NotificationStatus> statuses)
    {
        while (_status.getRowCount() > 0) {
            _status.removeRow(_status.getRowCount() - 1);
        }

        if (statuses.size() == 0) {
            _status.setText(0, 0, _msgs.fbNotifsStatusEmpty());
            return;
        }

        _status.setText(0, 0, _msgs.fbNotifsIdHdr());
        _status.setText(0, 1, _msgs.fbNotifsStatusHdr());
        _status.setText(0, 2, _msgs.fbNotifsStartHdr());
        _status.setText(0, 3, _msgs.fbNotifsFinishedHdr());
        _status.setText(0, 4, _msgs.fbNotifsUserCountHdr());
        _status.setText(0, 5, _msgs.fbNotifsSentCountHdr());
        for (NotificationStatus status : statuses) {
            int row = _status.addText(status.id, 1);
            _status.setText(row, 1, status.status);
            _status.setText(row, 2, fmtDate(status.start));
            _status.setText(row, 3, fmtDate(status.finished));
            _status.setText(row, 4, String.valueOf(status.userCount));
            _status.setText(row, 5, String.valueOf(status.sentCount));
        }
    }

    protected String fmtDate (Date date)
    {
        String value = null;
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

    protected TextBox _delay, _id;
    protected TextArea _message;
    protected SmartTable _status, _saved;
}
