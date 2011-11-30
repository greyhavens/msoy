//
// $Id$

package com.threerings.msoy.tutorial.client {

import mx.core.UIComponent;

import com.threerings.util.Util;

/**
 * Builder for tutorial items. Constructed by the tutorial director with all required item fields
 * in place. After desired mutation of values, the item may be queued to the director.
 */
public class TutorialItemBuilder
{
    /**
     * Creates a new builder for the given item. The given director is used to queue up the item.
     */
    public function TutorialItemBuilder (item :TutorialItem, director :TutorialDirector,
                                         sequenceBuilder :TutorialSequenceBuilder = null)
    {
        _item = item;
        _director = director;
        _sequenceBuilder = sequenceBuilder;
    }

    /**
     * Sets the limiting function for the item we are building.
     * @param checkAvailable the predicate to test if the item should still be available, for
     *        example a room publishing suggestion should not popup if the user has gone into a
     *        game. The prototype of the function should that of a standard predicate, as follows:
     *        <listing>
     *            function availableFn () :Boolean;
     *        </listing>
     */
    public function limit (checkAvailable :Function) :TutorialItemBuilder
    {
        _item.checkAvailable = checkAvailable;
        return this;
    }

    /**
     * Sets the popup helper to be invoked when the item is popped up and down.
     */
    public function popup (helper :PopupHelper) :TutorialItemBuilder
    {
        _item.popupHelper = helper;
        return this;
    }

    /**
     * Sets the popup helper to a ui highlighter that will highlight the given ui component using a
     * standard tutorial graphic on popup and unhighlight it on popdown.
     */
    public function highlightObj (obj :UIComponent) :TutorialItemBuilder
    {
        return popup(new UIHighlightHelper(_director.topPanel, obj));
    }

    /**
     * Sets the popup helper to a ui highlighter that will highlight the ui component returned by
     * the given function. The returned value may change over time and a null return value will
     * cause the highlight to disppear.
     */
    public function highlightFn (fn :Function) :TutorialItemBuilder
    {
        return popup(new UIHighlightHelper(_director.topPanel, fn));
    }

    /**
     * Sets the popup helper to a ui highlighter that will highlight the given control bar ui
     * component using a standard tutorial graphic on popup and unhighlight it on popdown. This is
     * different from a normal highlight in that the menu popper will be highlighted if the
     * desired component is not currently being displayed.
     */
    public function controlBarHighlight (obj :UIComponent) :TutorialItemBuilder
    {
        return popup(new UIHighlightHelper(_director.topPanel, Util.adapt(
            _director.topPanel.getControlBar().getClickableComponent, obj)));
    }

    /**
     * Sets the popup helper to highlight the menu item designated by the given command in the
     * menu that pops up from the given button. Until the menu is opened, the button is
     * highlighted. If the button is obscured, the control bar palette toggle is highlighted.
     */
    public function menuItemHighlight (obj :UIComponent, command :String) :TutorialItemBuilder
    {
        return popup(new MenuHighlightHelper(_director.worldCtx, Util.adapt(
            _director.topPanel.getControlBar().getClickableComponent, obj), command));
    }

    /**
     * Limit the item for display exclusively to new users.
     */
    public function newbie () :TutorialItemBuilder
    {
        _levels = Levels.NEWBIE;
        return this;
    }

    /**
     * Limit the item for display exclusively to beginner users.
     */
    public function beginner () :TutorialItemBuilder
    {
        _levels = Levels.BEGINNER;
        return this;
    }

    /**
     * Limit the item for display exclusively to intermediate users.
     */
    public function intermediate () :TutorialItemBuilder
    {
        _levels = Levels.INTERMEDIATE;
        return this;
    }

    /**
     * Limit the item for display exclusively to advanced users.
     */
    public function advanced () :TutorialItemBuilder
    {
        _levels = Levels.ADVANCED;
        return this;
    }

    /**
     * Adds a button for the item we are building.
     * @param text the text of the button, for example "Show Me"
     * @param onClick the function to call when the button is pressed. The function prototype
              should be paramterless and return void, as follows:
     *        <listing>
     *            function buttonFn () :void;
     *        </listing>
     */
    public function button (text :String, onClick :Function) :TutorialItemBuilder
    {
        _item.buttonText = text;
        _item.onClick = onClick;
        return this;
    }

    /**
     * Sets up the item so that pressing the button also changes the text in the popup and hides
     * the button disappears. This may not be used with <code>buttonCloses</code>.
     */
    public function finishText (text :String) :TutorialItemBuilder
    {
        _item.finishText = text;
        return this;
    }

    /**
     * Sets up the item so that pressing the button also dismisses the popup.
     * @param hideClose Sets the close button to be hidden.
     */
    public function buttonCloses (hideClose :Boolean = false) :TutorialItemBuilder
    {
        _item.buttonCloses = true;
        _item.hideClose = hideClose;
        return this;
    }

    /**
     * Sets a flag on the item that prevents it from being ignored.
     */
    public function noIgnore () :TutorialItemBuilder
    {
        _item.ignorable = false;
        return this;
    }

    /**
     * Adds a function to be called when this item is shown.
     */
    public function onShow (onShow :Function) :TutorialItemBuilder
    {
        var existing :Function = _item.onShow;
        _item.onShow = (existing == null || onShow == null) ? onShow : function () :void {
            existing();
            onShow();
        };
        return this;
    }

    /**
     * Queues up the item to be displayed. The builder may no longer be used after this.
     */
    public function queue () :void
    {
        // chain the level availability function, if any, onto the caller-provided one
        _item.checkAvailable = Levels.makeCheck(
            _levels, _director.getMemberLevel, _item.checkAvailable);
        if (_sequenceBuilder != null) {
            _sequenceBuilder.queue(_item);
        } else {
            _director.queueItem(_item);
        }
        _item = null;
        _director = null;
        _sequenceBuilder = null;
    }

    public function getId () :String
    {
        return _item.id;
    }

    protected var _item :TutorialItem;
    protected var _levels :Levels;
    protected var _director :TutorialDirector;
    protected var _sequenceBuilder :TutorialSequenceBuilder;
}
}
