//
// $Id$

package client.help;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.RoundBox;
import client.ui.TongueBox;

/**
 * Displays various help related information.
 */
public class HelpPanel extends VerticalPanel
{
    public HelpPanel ()
    {
        setStyleName("helpPanel");

        // header table includes report bug tab, help title, and thank you box
        SmartTable header = new SmartTable(0, 0);
        HTML reportBug = new HTML(CHelp.msgs.helpReportBug());
        reportBug.setStyleName("reportBug");
        header.setWidget(0, 0, reportBug);
        header.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);
        header.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        header.setWidget(0, 1, new Image("/images/help/help_header.png"), 1, "helpTitle");
        header.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        header.setWidget(1, 0, WidgetUtil.makeShim(10, 10), 2, "");
        header.setWidget(2, 0, new HTML(CHelp.msgs.helpIntro()), 2, "helpIntro");
        header.getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_CENTER);
        add(header);
        add(WidgetUtil.makeShim(10, 15));

        // wiki info
        add(new TongueBox(CHelp.msgs.helpWikiTitle(), CHelp.msgs.helpWiki(), true));
        add(WidgetUtil.makeShim(10, 10));

        // asking questions
        SmartTable questions = new SmartTable(0, 0);
        questions.setHTML(0, 0, CHelp.msgs.helpQuestionsIntro());
        questions.setWidget(1, 0, WidgetUtil.makeShim(5, 10));
        RoundBox faqBox = new RoundBox(RoundBox.MEDIUM_BLUE);
        faqBox.add(new HTML(CHelp.msgs.helpQuestions()));
        questions.setWidget(2, 0, faqBox);
        add(new TongueBox(CHelp.msgs.helpQuestionsTitle(), questions));
        add(WidgetUtil.makeShim(10, 10));

        // technical info
        add(new TongueBox(CHelp.msgs.helpTechTitle(), CHelp.msgs.helpTech(), true));

        // rules / terms of service / etc
        add(new TongueBox(CHelp.msgs.helpRulesTitle(), CHelp.msgs.helpRules(), true));
        add(WidgetUtil.makeShim(10, 15));

        // whirled team
        FlowPanel team = new FlowPanel();
        team.setStyleName("helpTeam");

        team.add(new HTML(CHelp.msgs.helpTeamEngineers()));
        List<String> helpTeamEngineersList =
            Arrays.asList(CHelp.msgs.helpTeamEngineersList().split(","));
        ColumnList helpTeamEngineers = new ColumnList(helpTeamEngineersList, 2);
        team.add(helpTeamEngineers);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(new HTML(CHelp.msgs.helpTeamArtists()));
        List<String> helpTeamArtistsList =
            Arrays.asList(CHelp.msgs.helpTeamArtistsList().split(","));
        ColumnList helpTeamArtists = new ColumnList(helpTeamArtistsList, 2);
        team.add(helpTeamArtists);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(new HTML(CHelp.msgs.helpTeamInfra()));
        List<String> helpTeamInfraList = Arrays.asList(CHelp.msgs.helpTeamInfraList().split(","));
        ColumnList helpTeamInfra = new ColumnList(helpTeamInfraList, 2);
        team.add(helpTeamInfra);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(new HTML(CHelp.msgs.helpTeamWaving()));
        List<String> helpTeamWavingList = Arrays.asList(CHelp.msgs.helpTeamWavingList().split(","));
        ColumnList helpTeamWaving = new ColumnList(helpTeamWavingList, 2);
        team.add(helpTeamWaving);

        add(new TongueBox(CHelp.msgs.helpTeamTitle(), team));
        add(WidgetUtil.makeShim(10, 20));
    }
}
