//
// $Id$

package client.adminz;

import client.util.ClickCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;

/**
 * Widget to display information about bureaus.
 */
public class BureauInfoPanel extends VerticalPanel
{
    /**
     * Creates a new bureau display.
     */
    public BureauInfoPanel ()
    {
        setStyleName("bureauInfoPanel");
        
        HorizontalPanel hpanel;
        add(hpanel = new HorizontalPanel());
        hpanel.setSpacing(10);
        hpanel.add(_requestRefresh = new Button(_msgs.bureauRequestRefresh()));
        hpanel.add(_refreshPage = new Button(_msgs.bureauRefreshPage()));
        add(_infoPanel = new VerticalPanel());
        _infoPanel.setSpacing(10);

        new ClickCallback<Void>(_requestRefresh) {
            @Override protected boolean callService () {
                _adminSvc.refreshBureauLauncherInfo(this);
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                return true;
            }
        };
        
        new ClickCallback<BureauLauncherInfo[]>(_refreshPage) {
            @Override protected boolean callService () {
                _adminSvc.getBureauLauncherInfo(this);
                return true;
            }

            @Override protected boolean gotResult (BureauLauncherInfo[] infos) {
                setInfo(infos);
                return true;
            }
        };
    }
    
    protected void setInfo (BureauLauncherInfo[] infos)
    {
        // TODO: show the common data at the top and individual bureaus beneath
        // TODO: show the running time or time since shutdown
        // TODO: show a table of contents and link to specific bureau info
        // TODO: show a kill button for each bureau
        _infoPanel.clear();
        FlexTable table;
        for (int ii = 0; ii < infos.length; ++ii) {
            BureauLauncherInfo info = infos[ii];
            for (int jj = 0; jj < info.bureaus.length; ++jj) {
                BureauLauncherInfo.BureauInfo binfo = info.bureaus[jj];
                int row = 0;
                _infoPanel.add(table = new FlexTable());
                table.setText(row, 0, _msgs.bureauHostname());
                table.setText(row++, 1, info.hostname);
                table.setText(row, 0, _msgs.bureauVersion());
                table.setText(row++, 1, String.valueOf(info.version));
                table.setText(row, 0, _msgs.bureauId());
                table.setText(row++, 1, binfo.bureauId);
                table.setText(row, 0, _msgs.bureauLogSpaceUsed());
                table.setText(row++, 1, String.valueOf(binfo.logSpaceUsed / 1024) + "kB");
                table.setText(row, 0, _msgs.bureauLogSpaceRemaining());
                table.setText(row++, 1, String.valueOf(binfo.logSpaceRemaining / 1024) + "kB");
            }
        }
        
        if (_infoPanel.getWidgetCount() == 0) {
            _infoPanel.add(new Label(_msgs.bureauNoneActive()));
        }
    }
    
    protected Button _requestRefresh;
    protected Button _refreshPage;
    protected VerticalPanel _infoPanel;

    protected AdminMessages _msgs = GWT.create(AdminMessages.class);

    protected AdminServiceAsync _adminSvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
