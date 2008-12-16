//
// $Id$

package client.adminz;

import java.util.HashSet;
import java.util.Set;

import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;

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
        
        _nodeList = new ListBox();
        _nodeList.setVisibleItemCount(10);
        _nodeList.addStyleName("nodeList");
        _nodeList.setMultipleSelect(true);
        addWidget(_nodeList, 1, null);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        setText(0, 1, _msgs.panRestartDescription());
        setWidget(1, 0, new Button(_msgs.panRestart(), new ClickListener() {
            public void onClick (Widget sender) {
                Set<String> nodes = new HashSet<String>();
                for (int i = 0; i < _nodeList.getItemCount(); i++) {
                    if (_nodeList.isItemSelected(i)) {
                        nodes.add(_nodeList.getValue(i));
                    }
                }
                _adminsvc.restartPanopticon(nodes, new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        // Give the logger about 3 seconds to restart before refreshing.
                        new Timer() {
                            public void run () {
                                refresh();
                            }
                        }.schedule(3000);
                    }
                });
            }
        }), 2, null);
        
        refresh();
    }
    
    public void refresh ()
    {
        _adminsvc.getPeerNodeNames(new MsoyCallback<Set<String>>() {
            public void onSuccess (Set<String> result) {
                _nodeList.clear();
                for (String node : result) {
                    _nodeList.addItem(node);
                }
            }
        });
        
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, "/status/panopticon");
        try {
            rb.sendRequest(null, new RequestCallback() {
                public void onError (Request request, Throwable exception) {
                    MsoyUI.error(_msgs.panStatusLoadError());
                }
                public void onResponseReceived (Request request, Response response) {
                    setText(2, 0, response.getText(), 2, "statusText");
                }
            });
        } catch (RequestException re) {
            MsoyUI.error(_msgs.panStatusRequestError());
        }
    }
    
    protected final ListBox _nodeList;
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
