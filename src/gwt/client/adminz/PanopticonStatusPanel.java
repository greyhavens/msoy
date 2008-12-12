//
// $Id$

package client.adminz;

import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Label;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.PanopticonStatus;

/**
 * This panel displays the status of a Panopticon client.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class PanopticonStatusPanel extends SmartTable
{
    public PanopticonStatusPanel ()
    {
        addStyleName("panopticonStatus");
        
        int row = addText(_msgs.panTimeStarted(), 0, "header");
        setWidget(row, 1, _timeStarted = new Label());
        
        row = addText(_msgs.panQueuedEvents(), 0, "header");
        setWidget(row, 1, _currentlyQueued = new Label());
        
        row = addText(_msgs.panDroppedEvents(), 0, "header");
        setWidget(row, 1, _dropped = new Label());
        
        row = addText(_msgs.panEventsSent(), 0, "header");
        setWidget(row, 1, _totalSent = new Label());
        
        row = addText(_msgs.panOverflowed(), 0, "header");
        setWidget(row, 1, _overflowed = new Label());
        
        row = addText(_msgs.panLastTimeEnteredRetryMode(), 0, "header");
        setWidget(row, 1, _lastTimeEnteredRetryMode = new Label());
        row = addText(_msgs.panLastTimeRecoveredFromRetryMode(), 0, "header");
        setWidget(row, 1, _lastTimeRecoveredFromRetryMode = new Label());
        row = addText(_msgs.panLastTempFailed(), 0, "header");
        setWidget(row, 1, _lastTimeTempFailed = new Label());
        
        row = addText(_msgs.panLastTempFailure(), 0, "header");
        row = addWidget(_lastTempFailureInfo = new Label(), 0, null);
        row = addText(_msgs.panLastPermFailure(), 0, "header");
        row = addWidget(_lastPermFailureInfo = new Label(), 3, null);
        
        refresh();
    }
    
    public void refresh ()
    {
        _adminsvc.getPanopticonStatus(new MsoyCallback<PanopticonStatus>() {
            public void onSuccess (PanopticonStatus result) {
                loadStatus(result);
            }
        });
    }
    
    protected void loadStatus (PanopticonStatus status)
    {
        _currentlyQueued.setText(String.valueOf(status.currentlyQueued) + ", total: " + 
            String.valueOf(status.totalQueued) + (status.lastTimeQueued == null ? "" : ", last: " + 
            DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeQueued)));
        _dropped.setText(String.valueOf(status.dropped) + (status.lastTimeDropped == null ? "" : 
            ", last: " + DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeDropped)));
        _totalSent.setText(String.valueOf(status.totalSent) + (status.lastTimeSent == null ? "" : 
            ", last: " +DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeSent)));
        _overflowed.setText(String.valueOf(status.overflowed) + 
            (status.lastTimeOverflowed == null ? "" : ", last: " +
            DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeOverflowed)) +
            (status.lastTimeQueueOverflowed == null ? "" : ", last requeued: " +
            DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeQueueOverflowed)));
        _timeStarted.setText(status.timeStarted == null ? "" : 
            DateTimeFormat.getMediumDateTimeFormat().format(status.timeStarted));
        _lastTimeEnteredRetryMode.setText(status.lastTimeEnteredRetryMode == null ? "" : 
            DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeEnteredRetryMode));
        _lastTimeRecoveredFromRetryMode.setText(status.lastTimeRecoveredFromRetryMode == null ? "" : 
            DateTimeFormat.getMediumDateTimeFormat().format(status.lastTimeRecoveredFromRetryMode));
        _lastTempFailureInfo.setText(status.lastTempFailureInfo);
        _lastPermFailureInfo.setText(status.lastPermFailureInfo);
    }
    
    protected final Label _currentlyQueued;
    protected final Label _dropped;
    protected final Label _totalSent;
    protected final Label _overflowed;
    protected final Label _timeStarted;
    protected final Label _lastTimeEnteredRetryMode;
    protected final Label _lastTimeRecoveredFromRetryMode;
    protected final Label _lastTimeTempFailed;
    protected final Label _lastTempFailureInfo;
    protected final Label _lastPermFailureInfo;
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
