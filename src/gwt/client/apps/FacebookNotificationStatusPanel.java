//
// $Id$

package client.apps;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;
import com.threerings.msoy.apps.gwt.FacebookNotificationStatus;

import client.ui.MsoyUI;
import client.util.InfoCallback;

public class FacebookNotificationStatusPanel extends FlowPanel
{
    public FacebookNotificationStatusPanel (AppInfo info)
    {
        addStyleName("facebookNotificationStatus");
        _appId = info.appId;

        add(MsoyUI.createHTML(_msgs.fbNotifStatusTitle(info.name), "Title"));

        final StatusList status = new StatusList();
        add(status);

        add(new Button(_msgs.fbNotifStatusRefresh(), new ClickHandler () {
            public void onClick (ClickEvent event) {
                status.refresh();
            }
        }));
    }

    protected class StatusList extends SmartTable
    {
        public StatusList ()
        {
            super("statusList", 5, 0);
            setWidget(0, 0, MsoyUI.createNowLoading());
            refresh();
        }

        public void refresh ()
        {
            _appsvc.loadNotificationsStatus(_appId,
                new InfoCallback<List<FacebookNotificationStatus>> () {
                    @Override public void onSuccess (List<FacebookNotificationStatus> result) {
                        setNotifications(result);
                    }
                });
        }

        public void setNotifications (List<FacebookNotificationStatus> statuses)
        {
            while (getRowCount() > 0) {
                removeRow(getRowCount() - 1);
            }
    
            if (statuses.size() == 0) {
                setText(0, 0, _msgs.fbNotifStatusEmpty());
                return;
            }
    
            final int BATCH_ID = 0, PROGRESS = 1, START_TIME = 2, USER_COUNT=3, SENT_COUNT = 4;
    
            int row = 0;
            setText(row, BATCH_ID, _msgs.fbNotifStatusBatchIdHdr(), 1, "Header", "BatchId");
            setText(row, PROGRESS, _msgs.fbNotifStatusProgressHdr(), 1, "Header");
            setText(row, START_TIME, _msgs.fbNotifStatusStartTimeHdr(), 1, "Header");
            setText(row, USER_COUNT, _msgs.fbNotifStatusUserCountHdr(), 1, "Header");
            setText(row, SENT_COUNT, _msgs.fbNotifStatusSentCountHdr(), 1, "Header");
            getRowFormatter().setStyleName(row++, "Row");
            for (FacebookNotificationStatus status : statuses) {
                setText(row, BATCH_ID, status.batchId, 1, "BatchId");
                setText(row, PROGRESS, status.progress);
                setText(row, START_TIME, fmtDate(status.startTime));
                setText(row, USER_COUNT, String.valueOf(status.userCount));
                setText(row, SENT_COUNT, String.valueOf(status.sentCount));
                getRowFormatter().setStyleName(row, "Row");
                if (row % 2 == 1) {
                    getRowFormatter().addStyleName(row, "AltRow");
                }
                row++;
            }
        }
    }

    protected static String fmtDate (Date date)
    {
        String value = "-";
        if (date != null) {
            value = DateUtil.formatDateTime(date);
        }
        return value;
    }

    protected int _appId;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
