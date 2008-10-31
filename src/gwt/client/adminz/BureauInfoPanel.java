//
// $Id$

package client.adminz;

import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
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
        
        ClickListener requestRefresh = new ClickListener() {
            public void onClick (Widget widget) {
                requestRefresh().setEnabled(false);
                _adminSvc.refreshBureauLauncherInfo(new AsyncCallback<Void>() {
                    public void onFailure (Throwable caught) {
                        requestRefresh().setEnabled(true);
                    }

                    public void onSuccess (Void unused) {
                        requestRefresh().setEnabled(true);
                    }
                });
            }
        };
        
        ClickListener refreshPage = new ClickListener() {
            public void onClick (Widget widget) {
                refreshPage().setEnabled(false);
                _adminSvc.getBureauLauncherInfo(new AsyncCallback<BureauLauncherInfo[]>() {
                    public void onFailure (Throwable caught) {
                        refreshPage().setEnabled(true);
                        infoPanel().clear();
                        infoPanel().add(new Label(_msgs.bureauServiceFailed()));
                    }

                    public void onSuccess (BureauLauncherInfo[] result) {
                        refreshPage().setEnabled(true);
                        setInfo(result);
                    }
                });
            }
        };

        // The order of the adds is important
        HorizontalPanel hpanel;
        add(hpanel = new HorizontalPanel());
        hpanel.setSpacing(10);
        hpanel.add(new Button(_msgs.bureauRequestRefresh(), requestRefresh));
        hpanel.add(new Button(_msgs.bureauRefreshPage(), refreshPage));
        add(new VerticalPanel());
        infoPanel().setSpacing(10);

        // Initial refresh to see what the server has already
        refreshPage.onClick(null);
    }
    
    protected void setInfo (BureauLauncherInfo[] infos)
    {
        // TODO: show the common data at the top and individual bureaus beneath
        // TODO: show the running time or time since shutdown
        // TODO: show a table of contents and link to specific bureau info
        // TODO: show a kill button for each bureau
        infoPanel().clear();
        FlexTable table;
        for (int ii = 0; ii < infos.length; ++ii) {
            BureauLauncherInfo info = infos[ii];
            for (int jj = 0; jj < info.bureaus.length; ++jj) {
                BureauLauncherInfo.BureauInfo binfo = info.bureaus[jj];
                int row = 0;
                infoPanel().add(table = new FlexTable());
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
        
        if (infoPanel().getWidgetCount() == 0) {
            infoPanel().add(new Label(_msgs.bureauNoneActive()));
        }
    }
    
    protected HorizontalPanel buttonBar ()
    {
        return (HorizontalPanel)getWidget(0);
    }
    
    protected Button requestRefresh ()
    {
        return (Button)buttonBar().getWidget(0);
    }

    protected Button refreshPage ()
    {
        return (Button)buttonBar().getWidget(1);
    }

    protected VerticalPanel infoPanel ()
    {
        return (VerticalPanel)getWidget(1);
    }

    protected AdminMessages _msgs = GWT.create(AdminMessages.class);

    protected AdminServiceAsync _adminSvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
