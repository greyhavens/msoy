//
// $Id$

package client.ui;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;

import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup with a nice border around it.
 */
public class BorderedPopup extends PopupPanel
{
    public BorderedPopup ()
    {
        this(false);
    }

    public BorderedPopup (boolean autoHide)
    {
        this(autoHide, false);
    }

    public BorderedPopup (boolean autoHide, boolean modal)
    {
        super(autoHide, modal);
        setStyleName("borderedPopup");

        super.setWidget(_widget = new BorderedWidget());

        // listen for our own closes and export that in a handy calldown method
        _registration = addCloseHandler(_closeHandler);

        // start out with animation enabled for our first pop
        setAnimationEnabled(true);
    }

    @Override // from SimplePanel
    public void setWidget (Widget contents)
    {
        _widget.setWidget(contents);
    }

    @Override // from PopupPanel
    public void show ()
    {
        if (_centerOnShow) {
            _centerOnShow = false;
            _registration.removeHandler();
            center(); // this will show us
            _registration = addCloseHandler(_closeHandler);
        } else {
            super.show();
        }

        // then turn it off because otherwise dragging re-triggers the animation
        setAnimationEnabled(false);
    }

    /**
     * Called when this popup is dismissed.
     */
    protected void onClosed (boolean autoClosed)
    {
    }

    /**
     * This must be called any time a popup's dimensions change to update the hacky iframe that
     * ensures that the popup is visible over Flash or Java applets. It is called automatically
     * when the popup position changes, but there is no way to do it magically for size changes.
     */
    protected void updateFrame ()
    {
        // if I could access 'impl' here, I wouldn't have to do this lame hack, but the GWT
        // engineers conveniently made it private, so I can't
        _registration.removeHandler();
        hide();
        super.show();
        _registration = addCloseHandler(_closeHandler);
    }

    protected HandlerRegistration _registration;
    protected CloseHandler<PopupPanel> _closeHandler = new CloseHandler<PopupPanel>() {
        public void onClose (CloseEvent<PopupPanel> event) {
            onClosed(event.isAutoClosed());
        }
    };

    protected BorderedWidget _widget;

    /** Set this to false to disable the default centering. */
    protected boolean _centerOnShow = true;
}
