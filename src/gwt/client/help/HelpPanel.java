//
// $Id$

package client.help;

import java.util.Date;

import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.util.TongueBox;

/**
 * Displays various help related information.
 */
public class HelpPanel extends VerticalPanel
{
    public HelpPanel ()
    {
        add(new TongueBox(null, CHelp.msgs.helpIntro(), false));
        add(new TongueBox(CHelp.msgs.helpQuestionsTitle(), CHelp.msgs.helpQuestions(), true));
        add(new TongueBox(CHelp.msgs.helpBugsTitle(), CHelp.msgs.helpBugs(), true));
        add(new TongueBox(CHelp.msgs.helpWikiTitle(), CHelp.msgs.helpWiki(), true));

        // TODO: add a way to restart the tutorial

        SmartTable credits = new SmartTable(0, 0);
        int row = 0;
        credits.setHTML(row++, 0, CHelp.msgs.helpTeamEngineers());
        credits.setWidget(row++, 0, WidgetUtil.makeShim(5, 10));
        credits.setHTML(row++, 0, CHelp.msgs.helpTeamArtists());
        credits.setWidget(row++, 0, WidgetUtil.makeShim(5, 10));
        credits.setHTML(row++, 0, CHelp.msgs.helpTeamDPW());
        credits.setWidget(row++, 0, WidgetUtil.makeShim(5, 10));
        credits.setHTML(row++, 0, CHelp.msgs.helpTeamWaving());
        credits.setWidget(row++, 0, WidgetUtil.makeShim(5, 10));
        credits.setHTML(row++, 0, CHelp.msgs.helpCopyright("" + (1900 + new Date().getYear())));
        add(new TongueBox(CHelp.msgs.helpTeamTitle(), credits));
    }
}
