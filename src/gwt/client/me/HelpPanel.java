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
        Frame.setTitle(CWhirled.msgs.helpTitle());

        int row = 0;
        setText(row++, 0, CWhirled.msgs.helpIntro());

        setText(row++, 0, CWhirled.msgs.helpQuestionsTitle(), 1, "Title");
        setHTML(row++, 0, CWhirled.msgs.helpQuestions());

        setText(row++, 0, CWhirled.msgs.helpBugsTitle(), 1, "Title");
        setHTML(row++, 0, CWhirled.msgs.helpBugs());

        setText(row++, 0, CWhirled.msgs.helpWikiTitle(), 1, "Title");
        setHTML(row++, 0, CWhirled.msgs.helpWiki());

        // TODO: add a way to restart the tutorial

        setText(row++, 0, CWhirled.msgs.helpTeamTitle(), 1, "Title");
        setHTML(row++, 0, CWhirled.msgs.helpTeamEngineers());
        setHTML(row++, 0, CWhirled.msgs.helpTeamArtists());
        setHTML(row++, 0, CWhirled.msgs.helpTeamDPW());
        setHTML(row++, 0, CWhirled.msgs.helpTeamWaving());

        setHTML(row++, 0, CWhirled.msgs.helpCopyright("" + (1900 + new Date().getYear())));
    }
}
