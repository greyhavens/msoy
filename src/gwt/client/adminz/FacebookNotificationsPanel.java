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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.facebook.gwt.NotificationStatus;

import client.ui.MsoyUI;
import client.util.InfoCallback;

public class FacebookNotificationsPanel extends FlowPanel
{
    public FacebookNotificationsPanel ()
    {
        setStyleName("facebookNotifications");
        add(MsoyUI.createLabel(_msgs.fbNotifsInstructions(), "Instructions"));
        add(MsoyUI.createFlowPanel("Form",
            makeRow(_msgs.fbNotifsIdLabel(), _id = MsoyUI.createTextBox("manual", 24, 24)),
            makeRow(_msgs.fbNotifsMessageLabel(), _message = MsoyUI.createTextArea("", 50, 6)),
            makeRow(_msgs.fbNotifsDelayLabel(), _delay = MsoyUI.createTextBox("1", 3, 3),
                _msgs.fbNotifsDelayUnits()),
            makeRow("", new Button(_msgs.fbNotifsScheduleBtn(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    send();
                }
            })),
            makeRow(_msgs.fbNotifsStatus(), new Button(_msgs.fbNotifsRefresh(),
                new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _svc.getFacebookNotificationStatuses(new 
                        InfoCallback<List<NotificationStatus>>() {
                        public void onSuccess (List<NotificationStatus> result) {
                            setStatuses(result);
                        }
                    });
                }
            }))));
        add(_status = new SimplePanel());
        _status.setStyleName("Status");
    }

    protected void send ()
    {
        String id = _id.getText().trim();
        if (id.equals("")) {
            MsoyUI.error(_msgs.fbNotifsEmptyIdErr());
            return;
        }
        String msg = _message.getText().trim();
        if (msg.equals("")) {
            MsoyUI.error(_msgs.fbNotifsEmptyMessageErr());
            return;
        }
        int delay;
        try {
            delay = Integer.parseInt(_delay.getText());
        } catch (Exception e) {
            MsoyUI.error(_msgs.fbNotifsDelayFmtErr());
            return;
        }
        _svc.sendFacebookNotification(id, msg, delay, new InfoCallback<Void> () {
            public void onSuccess (Void result) {
                MsoyUI.info(_msgs.fbNotifsScheduled());
            }
        });
    }

    protected void setStatuses (List<NotificationStatus> statuses)
    {
        if (statuses.size() == 0) {
            _status.setWidget(MsoyUI.createLabel(_msgs.fbNotifsStatusEmpty(), null));
            return;
        }
        FlowPanel panel = new FlowPanel();
        _status.setWidget(panel);
        for (NotificationStatus status : statuses) {
            panel.add(makeRow(_msgs.fbNotifsStatusId(), status.id));
            panel.add(makeRow(_msgs.fbNotifsStatusStatus(), status.status));
            panel.add(makeRow(_msgs.fbNotifsStatusStart(), status.start));
            panel.add(makeRow(_msgs.fbNotifsStatusFinished(), status.finished));
            panel.add(makeRow(_msgs.fbNotifsStatusUserCount(), String.valueOf(status.userCount)));
            panel.add(WidgetUtil.makeShim(10, 10));
        }
    }

    protected Widget makeRow (String name, Date date)
    {
        String value = null;
        if (date != null) {
            value = DateUtil.formatDateTime(date);
        }
        return makeRow(name, value);
    }

    protected Widget makeRow (String name, String value)
    {
        if (value == null) {
            value = "N/A";
        }
        return makeRow(name, MsoyUI.createLabel(value, "Value"), null);
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
    protected SimplePanel _status;

    protected static final AdminServiceAsync _svc = GWT.create(AdminService.class);
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
}
