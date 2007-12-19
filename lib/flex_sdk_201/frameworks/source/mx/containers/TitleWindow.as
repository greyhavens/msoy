////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers
{

import mx.core.mx_internal;

use namespace mx_internal;

//--------------------------------------
//  Events
//--------------------------------------

/**
 *  Dispatched when the user selects the close button.
 *
 *  @eventType mx.events.CloseEvent.CLOSE
 *  @helpid 3985
 *  @tiptext close event
 */
[Event(name="close", type="mx.events.CloseEvent")]

//--------------------------------------
//  Styles
//--------------------------------------

/**
 *  The close button disabled skin.
 *
 *  @default CloseButtonDisabled
 */
[Style(name="closeButtonDisabledSkin", type="Class", inherit="no")]

/**
 *  The close button down skin.
 *
 *  @default CloseButtonDown
 */
[Style(name="closeButtonDownSkin", type="Class", inherit="no")]

/**
 *  The close button over skin.
 *
 *  @default CloseButtonOver
 */
[Style(name="closeButtonOverSkin", type="Class", inherit="no")]

/**
 *  The close button up skin.
 *
 *  @default CloseButtonUp
 */
[Style(name="closeButtonUpSkin", type="Class", inherit="no")]

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="focusIn", kind="event")]
[Exclude(name="focusOut", kind="event")]

[Exclude(name="focusBlendMode", kind="style")]
[Exclude(name="focusSkin", kind="style")]
[Exclude(name="focusThickness", kind="style")]

[Exclude(name="focusInEffect", kind="effect")]
[Exclude(name="focusOutEffect", kind="effect")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[AccessibilityClass(implementation="mx.accessibility.TitleWindowAccImpl")]

[IconFile("TitleWindow.png")]

/**
 *  A TitleWindow navigator container contains a title bar, a caption,
 *  a border, and a content area for its child.
 *  Typically, you use TitleWindow containers to wrap self-contained
 *  application modules.
 *  For example, you could include a form in a TitleWindow container.
 *  When the user completes the form, you can close the TitleWindow
 *  container programmatically, or let the user close it by using the
 *  Close button.
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:TitleWindow&gt;</code> tag inherits all of the tag 
 *  attributes of its superclass, and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:TitleWindow
 *   <b>Properties</b>
 *   showCloseButton="false|true"
 *   closeButtonDisabledSkin="CloseButtonDisabled"
 *   closeButtonDownSkin="CloseButtonDown"
 *   closeButtonOverSkin="CloseButtonOver"
 *   closeButtonUpSkin="CloseButtonUp"
 *   <strong>Events</strong>
 *   close="<i>No default</i>"
 *   &gt;
 *    ...
 *      child tags
 *    ...
 *  /&gt;
 *  </pre>
 *  
 *  @includeExample examples/SimpleTitleWindowExample.mxml -noswf
 *  @includeExample examples/TitleWindowApp.mxml
 *  
 *  @see mx.core.Application
 *  @see mx.managers.PopUpManager
 *  @see mx.containers.Panel
 */
public class TitleWindow extends Panel
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class mixins
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  Placeholder for mixin by TitleWindowAccImpl.
     */
    mx_internal static var createAccessibilityImplementation:Function;

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     */
    public function TitleWindow()
    {
        super();
    }

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  showCloseButton
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  Whether to display a Close button in the TitleWindow container.
     *  The default value is <code>false</code>.
     *  Set it to <code>true</code> to display the Close button.
     *  Selecting the Close button generates a <code>close</code> event,
     *  but does not close the TitleWindow container.
     *  You must write a handler for the <code>close</code> event
     *  and close the TitleWindow from within it.
     *
     *  @default false
     *
     *  @tiptext If true, the close button is displayed
     *  @helpid 3986
     */
    public function get showCloseButton():Boolean
    {
        return mx_internal::_showCloseButton;
    }

    /**
     *  @private
     */
    public function set showCloseButton(value:Boolean):void
    {
        mx_internal::_showCloseButton = value;
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden methods: UIComponent
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function initializeAccessibility():void
    {
        if (TitleWindow.createAccessibilityImplementation != null)
            TitleWindow.createAccessibilityImplementation(this);
    }
}

}
