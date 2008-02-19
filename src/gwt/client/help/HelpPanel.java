//
// $Id$

package client.help;

import java.util.Date;

import com.threerings.gwt.ui.SmartTable;

/**
 * Displays various help related information.
 */
public class HelpPanel extends SmartTable
{
    public HelpPanel ()
    {
        super("helpPanel", 0, 10);

        int row = 0;
        setText(row++, 0, CHelp.msgs.helpIntro());

        setText(row++, 0, CHelp.msgs.helpQuestionsTitle(), 1, "Title");
        setHTML(row++, 0, CHelp.msgs.helpQuestions());

        setText(row++, 0, CHelp.msgs.helpBugsTitle(), 1, "Title");
        setHTML(row++, 0, CHelp.msgs.helpBugs());

        setText(row++, 0, CHelp.msgs.helpWikiTitle(), 1, "Title");
        setHTML(row++, 0, CHelp.msgs.helpWiki());

        // TODO: add a way to restart the tutorial

        setText(row++, 0, CHelp.msgs.helpTeamTitle(), 1, "Title");
        setHTML(row++, 0, CHelp.msgs.helpTeamEngineers());
        setHTML(row++, 0, CHelp.msgs.helpTeamArtists());
        setHTML(row++, 0, CHelp.msgs.helpTeamDPW());
        setHTML(row++, 0, CHelp.msgs.helpTeamWaving());

        setHTML(row++, 0, CHelp.msgs.helpCopyright("" + (1900 + new Date().getYear())));
    }
}
