//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo.BureauInfo;

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
        hpanel.add(_refreshPage = new Button(_msgs.bureauRefresh()));
        add(_infoPanel = new VerticalPanel());
        _infoPanel.setSpacing(10);

        new ClickCallback<BureauLauncherInfo[]>(_refreshPage) {
            {
                takeAction(true);
            }
            @Override protected boolean callService () {
                _adminSvc.getBureauLauncherInfo(this);
                return true;
            }
            @Override protected boolean gotResult (BureauLauncherInfo[] infos) {
                refresh(infos);
                return true;
            }
        };
    }

    protected void refresh (BureauLauncherInfo[] infos)
    {
        // TODO: show the running time or time since shutdown
        // TODO: show a table of contents and link to specific bureau info
        // TODO: show a kill button for each bureau
        // TODO: have the server also resolve the names of the games
        _infoPanel.clear();
        _infoPanel.add(new Label(_msgs.bureauLaunchersTitle()));
        _infoPanel.add(makeLaunchersPanel(infos));
        _infoPanel.add(new Label(_msgs.bureauBureausTitle()));
        _infoPanel.add(makeBureausPanel(infos));
    }

    protected Widget makeLaunchersPanel (BureauLauncherInfo[] infos)
    {
        if (infos.length == 0) {
            return new Label(_msgs.bureauNoLaunchers());
        }

        SmartTable table = new SmartTable();
        table.setStyleName("info");
        int row = 0;

        table.setText(row, 0, _msgs.bureauHostname());
        table.setText(row, 1, _msgs.bureauError());
        table.setText(row++, 2, _msgs.bureauConnections());

        for (int ii = 0; ii < infos.length; ++ii) {
            BureauLauncherInfo info = infos[ii];
            table.setText(row, 0, info.hostname);
            table.setText(row, 1, info.error==null ? "" : info.error);
            StringBuilder connections = new StringBuilder();
            for (String connection : info.connections) {
                connections.append(connections.length() > 0 ? ", " : "");
                connections.append(connection);
            }
            table.setText(row++, 2, connections.toString());
        }

        return table;
    }

    protected Widget makeBureausPanel (BureauLauncherInfo[] infos)
    {
        Map<BureauInfo, BureauLauncherInfo> launchers =
            new HashMap<BureauInfo, BureauLauncherInfo>();
        List<BureauInfo> binfos = new ArrayList<BureauInfo>();
        for (BureauLauncherInfo info : infos) {
            for (BureauInfo binfo : info.bureaus) {
                binfos.add(binfo);
                launchers.put(binfo, info);
            }
        }

        if (binfos.size() == 0) {
            return new Label(_msgs.bureauNoBureaus());
        }

        Collections.sort(binfos, new Comparator<BureauInfo>() {
            public int compare (BureauInfo o1, BureauInfo o2) {
                return o1.bureauId.compareTo(o2.bureauId);
            }
        });

        SmartTable table = new SmartTable();
        table.setStyleName("info");

        int row = 0, col = 0;
        table.setText(row, col++, _msgs.bureauId());
        table.setText(row, col++, _msgs.bureauHost());
        table.setText(row, col++, _msgs.bureauLaunchTime());
        table.setText(row, col++, _msgs.bureauShutdownTime());
        table.setText(row, col++, _msgs.bureauLogSpaceUsed());
        table.setText(row, col++, _msgs.bureauLogSpaceRemaining());
        table.setText(row++, col++, _msgs.bureauMessage());

        for (BureauInfo binfo : binfos) {
            col = 0;
            table.setText(row, col++, binfo.bureauId);
            table.setText(row, col++, launchers.get(binfo).hostname);
            String launchTime = "";
            if (binfo.launchTime != 0) {
                launchTime = MsoyUI.formatDateTime(new Date(binfo.launchTime));
            }
            table.setText(row, col++, launchTime);
            String shutdownTime = _msgs.bureauFirstRun();
            if (binfo.shutdownTime != 0) {
                shutdownTime = MsoyUI.formatDateTime(new Date(binfo.shutdownTime));
            }
            table.setText(row, col++, shutdownTime);
            table.setText(row, col++, String.valueOf(binfo.logSpaceUsed / 1024) + "kB");
            table.setText(row, col++, String.valueOf(binfo.logSpaceRemaining / 1024) + "kB");
            table.setText(row, col++, binfo.message);
            table.getRowFormatter().setStyleName(row++, binfo.launchTime > binfo.shutdownTime ?
                "running" : "stopped");
        }

        return table;
    }

    protected Button _refreshPage;
    protected VerticalPanel _infoPanel;

    protected AdminMessages _msgs = GWT.create(AdminMessages.class);

    protected AdminServiceAsync _adminSvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
