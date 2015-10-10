//
// $Id$

package client.help;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.ui.MsoyUI;
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

        // // thanks and introduction
        // add(MsoyUI.createHTML(_msgs.helpIntro(), "helpIntro"));

        // // wiki info
        // add(new TongueBox(_msgs.helpWikiTitle(), _msgs.helpWiki(), true));
        // add(WidgetUtil.makeShim(10, 10));

        // asking questions
        SmartTable questions = new SmartTable(0, 0);
        questions.setWidget(0, 0, MsoyUI.createHTML(_msgs.helpQuestionsIntro(), null));
        questions.setWidget(1, 0, WidgetUtil.makeShim(5, 10));
        RoundBox faqBox = new RoundBox(RoundBox.MEDIUM_BLUE);
        faqBox.add(MsoyUI.createHTML(_msgs.helpQuestions(), null));
        questions.setWidget(2, 0, faqBox);
        add(new TongueBox(_msgs.helpQuestionsTitle(), questions));
        add(WidgetUtil.makeShim(10, 10));

        // technical info
        add(new TongueBox(_msgs.helpTechTitle(), _msgs.helpTech(), true));

        // rules / terms of service / etc
        add(new TongueBox(_msgs.helpRulesTitle(), _msgs.helpRules(), true));
        add(WidgetUtil.makeShim(10, 15));

        // whirled team
        FlowPanel team = new FlowPanel();
        team.setStyleName("helpTeam");

        team.add(MsoyUI.createHTML(_msgs.helpTeamEngineers(), null));
        List<String> helpTeamEngineersList =
            Arrays.asList(_msgs.helpTeamEngineersList().split(","));
        ColumnList helpTeamEngineers = new ColumnList(helpTeamEngineersList, 2);
        team.add(helpTeamEngineers);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(MsoyUI.createHTML(_msgs.helpTeamArtists(), null));
        List<String> helpTeamArtistsList =
            Arrays.asList(_msgs.helpTeamArtistsList().split(","));
        ColumnList helpTeamArtists = new ColumnList(helpTeamArtistsList, 2);
        team.add(helpTeamArtists);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(MsoyUI.createHTML(_msgs.helpTeamInfra(), null));
        List<String> helpTeamInfraList = Arrays.asList(_msgs.helpTeamInfraList().split(","));
        ColumnList helpTeamInfra = new ColumnList(helpTeamInfraList, 2);
        team.add(helpTeamInfra);
        team.add(WidgetUtil.makeShim(10, 10));

        team.add(MsoyUI.createHTML(_msgs.helpTeamWaving(), null));
        List<String> helpTeamWavingList = Arrays.asList(_msgs.helpTeamWavingList().split(","));
        ColumnList helpTeamWaving = new ColumnList(helpTeamWavingList, 2);
        team.add(helpTeamWaving);

        add(new TongueBox(_msgs.helpTeamTitle(), team));
        add(WidgetUtil.makeShim(10, 20));
    }

    protected static final HelpMessages _msgs = GWT.create(HelpMessages.class);
}
