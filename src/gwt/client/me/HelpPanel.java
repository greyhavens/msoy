//
// $Id$

package client.me;

import java.util.Date;

import com.threerings.gwt.ui.SmartTable;

import client.shell.Frame;

/**
 * Displays various help related information.
 */
public class HelpPanel extends SmartTable
{
    public HelpPanel ()
    {
        super("helpPanel", 0, 10);
        Frame.setTitle(CMe.msgs.helpTitle());

        int row = 0;
        setText(row++, 0, CMe.msgs.helpIntro());

        setText(row++, 0, CMe.msgs.helpQuestionsTitle(), 1, "Title");
        setHTML(row++, 0, CMe.msgs.helpQuestions());

        setText(row++, 0, CMe.msgs.helpBugsTitle(), 1, "Title");
        setHTML(row++, 0, CMe.msgs.helpBugs());

        setText(row++, 0, CMe.msgs.helpWikiTitle(), 1, "Title");
        setHTML(row++, 0, CMe.msgs.helpWiki());

        // TODO: add a way to restart the tutorial

        setText(row++, 0, CMe.msgs.helpTeamTitle(), 1, "Title");
        setHTML(row++, 0, CMe.msgs.helpTeamEngineers());
        setHTML(row++, 0, CMe.msgs.helpTeamArtists());
        setHTML(row++, 0, CMe.msgs.helpTeamDPW());
        setHTML(row++, 0, CMe.msgs.helpTeamWaving());

        setHTML(row++, 0, CMe.msgs.helpCopyright("" + (1900 + new Date().getYear())));
    }
}
