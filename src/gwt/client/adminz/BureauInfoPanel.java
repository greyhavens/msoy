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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo.BureauInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;

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
        // TODO: allow sorting by different things

        _infoPanel.clear();

        Label launchers = new Label(_msgs.bureauLaunchersTitle());
        launchers.setStyleName("title");
        _infoPanel.add(launchers);
        _infoPanel.add(makeLaunchersPanel(infos));

        Label bureaus = new Label(_msgs.bureauBureausTitle());
        bureaus.setStyleName("title");
        _infoPanel.add(bureaus);
        _infoPanel.add(makeBureausPanel(infos));
    }

    protected Widget makeLaunchersPanel (BureauLauncherInfo[] infos)
    {
        if (infos.length == 0) {
            return new Label(_msgs.bureauNoLaunchers());
        }

        SmartTable table = new SmartTable(5, 1);
        table.setStyleName("info");
        int row = 0;

        table.setText(row, 0, _msgs.bureauHostname());
        table.setText(row, 1, _msgs.bureauConnections());
        table.setText(row, 2, _msgs.bureauError());
        table.getRowFormatter().addStyleName(row++, "header");

        for (int ii = 0; ii < infos.length; ++ii) {
            BureauLauncherInfo info = infos[ii];
            table.setText(row, 0, getShortHostName(info.hostname));
            StringBuilder connections = new StringBuilder();
            for (String connection : info.connections) {
                connections.append(connections.length() > 0 ? ", " : "");
                connections.append(getShortHostName(connection));
            }
            table.setText(row, 1, connections.toString());
            table.setText(row++, 2, info.error==null ? "" : info.error);
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

        // sort so that running bureaus are first
        Collections.sort(binfos, new Comparator<BureauInfo>() {
            public int compare (BureauInfo o1, BureauInfo o2) {
                int cmp = Boolean.valueOf(o2.isRunning()).compareTo(
                    Boolean.valueOf(o1.isRunning()));
                return cmp == 0 ? o1.bureauId.compareTo(o2.bureauId) : cmp;
            }
        });

        SmartTable table = new SmartTable(5, 1);
        table.setStyleName("info");

        int row = 0, col = 0;
        table.setText(row, col++, _msgs.bureauId());
        table.setText(row, col++, _msgs.bureauGame());
        table.setText(row, col++, _msgs.bureauStatus());
        table.setText(row, col++, _msgs.bureauHost());
        table.setText(row, col++, _msgs.bureauTimes(), 2, null);
        table.setText(row, col++, _msgs.bureauLogSpace(), 2, null);
        //table.setText(row, col++, _msgs.bureauMessage());
        table.getRowFormatter().addStyleName(row++, "header");

        for (BureauInfo binfo : binfos) {
            col = 0;
            table.setText(row, col++, binfo.bureauId);
            if (binfo.gameId != 0) {
                table.setWidget(row, col++, Link.create(binfo.gameName, "Name",
                                                        Pages.GAMES, "d", binfo.gameId));
            } else {
                table.setText(row, col++, "");
            }
            boolean running = binfo.isRunning();
            table.setText(row, col, running ? _msgs.bureauRunning() : _msgs.bureauStopped());
            table.getFlexCellFormatter().setStyleName(row, col++, running ? "running" : "stopped");
            table.setText(row, col++, getShortHostName(launchers.get(binfo).hostname));
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
            table.setText(row++, col++, String.valueOf(binfo.logSpaceRemaining / 1024) + "kB");
            //table.setText(row++, col++, binfo.message);
            table.getRowFormatter().setStyleName(table.getRowCount()-1, "row" + (row%2));
        }

        return table;
    }

    protected static String getShortHostName (String host)
    {
        int didx = host.indexOf(".");
        return (didx == -1) ? host : host.substring(0, didx);
    }

    protected Button _refreshPage;
    protected VerticalPanel _infoPanel;

    protected AdminMessages _msgs = GWT.create(AdminMessages.class);

    protected AdminServiceAsync _adminSvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
