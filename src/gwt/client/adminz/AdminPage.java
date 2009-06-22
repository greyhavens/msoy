//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.Page;
import client.survey.EditSurveyPanel;
import client.survey.ViewSurveyResultsPanel;
import client.ui.MsoyUI;

/**
 * Displays an admin dashboard with various server status information and administrative
 * functionality.
 */
public class AdminPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (CShell.isGuest()) {
            setContent(MsoyUI.createLabel(_msgs.indexLogon(), "infoLabel"));

        } else if (!CShell.isSupport()) {
            setContent(MsoyUI.createLabel(_msgs.lackPrivileges(), "infoLabel"));

        } else if (action.equals("review")) {
            byte type = (byte)args.get(1, 0);
            int item = args.get(2, 0);
            if (item != 0 && _reviewPanel != null) {
                ItemDetail detail = _reviewPanel.getItemDetail(type, item);
                if (detail == null) {
                    MsoyUI.error("Cannot view transactions for that item");
                    return;
                } else {
                    setContent(_msgs.reviewItemTransactionsTitle(),
                        new ItemTransactionsPanel(detail));
                }
            } else {
                if (_reviewPanel == null) {
                    _reviewPanel = new ReviewPanel();
                }
                setContent(_msgs.reviewTitle(), _reviewPanel);
            }

        } else if (action.equals("info")) {
            setContent(_msgs.infoTitle(), new MemberInfoPanel(args.get(1, 0)));

        } else if (action.equals("testlist")) {
            setContent(_msgs.abTestListTitle(), new ABTestListPanel());

        } else if (action.equals("test")) {
            setContent(_msgs.abTestTitle(), new ABTestSummaryPanel(args.get(1, 0)));

        } else if (action.equals("cashout")) {
            setContent(_msgs.cashOutTitle(), new BlingCashOutPanel());

        } else if (action.equals("exchange")) {
            setContent("Exchange", new ExchangePanel());

        } else if (action.equals("bureaus")) {
            setContent("Bureaus", new BureauInfoPanel());

        } else if (action.equals("promos")) {
            setContent("Promotions", new PromotionEditor());

        } else if (action.equals("contests")) {
            setContent(_msgs.contestsTitle(), new ContestsEditor());

        } else if (action.equals("stats")) {
            setContent("Stats", new StatsPanel());
            
        } else if (action.equals("panopticonStatus")) {
            setContent("Panopticon Status", new PanopticonStatusPanel());

        } else if (action.equals("survey") && args.get(1, "").equals("e")) {
            if (_surveyPanel == null) {
                _surveyPanel = new EditSurveyPanel();
            }
            _surveyPanel.setArgs(args);
            setContent("Surveys", _surveyPanel);

        } else if (action.equals("survey") && args.get(1, "").equals("r")) {
            setContent("Survey Results", new ViewSurveyResultsPanel(args.get(2, 0)));

        } else if (action.equals("broadcasts")) {
            setContent("Broadcasts", new BroadcastHistoryPanel());

        } else if (action.equals("entries")) {
            setContent("Entries", new EntrySummaryPanel());

        } else if (action.equals("facebook")) {
            setContent("Facebook Templates", new FacebookTemplatesPanel());

        } else {
            setContent(_msgs.title(), new DashboardPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ADMINZ;
    }

    protected ReviewPanel _reviewPanel;
    protected EditSurveyPanel _surveyPanel;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
}
