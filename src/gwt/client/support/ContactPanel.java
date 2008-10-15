//
// $Id$

package client.support;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.underwire.gwt.client.MyPetitionsPanel;
import com.threerings.underwire.gwt.client.NewPetitionPopup;
import com.threerings.underwire.gwt.client.UnderwireAuthListener;
import com.threerings.underwire.gwt.client.WebContext;

/**
 * Contact form that allows logged in users to send/manage petitions, and non-logged in guests to
 * send in a petition.
 */
public class ContactPanel extends FlowPanel
    implements WebContext.Page
{
    public ContactPanel (WebContext ctx)
    {
        _ctx = ctx;
        _ctx.page = this;

        add(_contents = new FlowPanel());
        if (_ctx.ainfo == null) {
            showMessagePanel();
        } else {
            showPetitionPanel();
        }
    }

    // from interface WebContext.Page
    public void pushContents (Widget widget)
    {
        _cstack.add(getWidget(getWidgetCount()-1));
        setContents(widget);
    }

    // from interface WebContext.Page
    public void popContents ()
    {
        if (_cstack.size() > 0) {
            setContents(_cstack.remove(_cstack.size()-1));
        } else {
            setContents(_contents);
        }
    }

    protected void showPetitionPanel ()
    {
        _petitions = new MyPetitionsPanel(_ctx);
        _contents.add(new NewPetitionBox(_ctx, _petitions));
        /*
        _submitNew = new Label(_ctx.cmsgs.submitNewPetition());
        _submitNew.setStyleName("uLinkLabel");
        _submitNew.addClickListener(_popper);
        _contents.add(_submitNew);
        */
        _contents.add(_petitions);
    }

    protected void showMessagePanel ()
    {
        _contents.add(new NewPetitionBox(_ctx, null));
    }

    protected void setContents (Widget contents)
    {
        remove(getWidgetCount()-1);
        add(contents);
    }

    protected ClickListener _popper = new ClickListener() {
        public void onClick (Widget source) {
            _ctx.page.pushContents(new NewPetitionPopup(_ctx, _petitions));
        }
    };

    protected WebContext _ctx;
    protected FlowPanel _contents;
    protected Label _submitNew;
    protected MyPetitionsPanel _petitions;

    protected List<Widget> _cstack = new ArrayList<Widget>();
}
