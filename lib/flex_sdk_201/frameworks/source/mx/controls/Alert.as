////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls
{

import flash.display.Sprite;
import mx.containers.Panel;
import mx.controls.alertClasses.AlertForm;
import mx.core.Application;
import mx.core.EdgeMetrics;
import mx.core.mx_internal;
import mx.events.CloseEvent;
import mx.managers.ISystemManager;
import mx.managers.PopUpManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;

use namespace mx_internal;

//--------------------------------------
//  Styles
//--------------------------------------

/**
 *  Name of the CSS style declaration that specifies 
 *  styles for the Alert buttons. 
 * 
 *  @default "alertButtonStyle"
 */
[Style(name="buttonStyleName", type="String", inherit="no")]

/**
 *  Name of the CSS style declaration that specifies
 *  styles for the Alert message text. 
 *
 *  <p>You only set this style by using a type selector, which sets the style 
 *  for all Alert controls in your application.  
 *  If you set it on a specific instance of the Alert control, it can cause the control to 
 *  size itself incorrectly.</p>
 * 
 *  @default undefined
 */
[Style(name="messageStyleName", type="String", inherit="no")]

/**
 *  Name of the CSS style declaration that specifies styles
 *  for the Alert title text. 
 *
 *  <p>You only set this style by using a type selector, which sets the style 
 *  for all Alert controls in your application.  
 *  If you set it on a specific instance of the Alert control, it can cause the control to 
 *  size itself incorrectly.</p>
 * 
 *  @default "windowStyles" 
 */
[Style(name="titleStyleName", type="String", inherit="no")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[AccessibilityClass(implementation="mx.accessibility.AlertAccImpl")]

[RequiresDataBinding(true)]

/**
 *  The Alert control is a pop-up dialog box that can contain a message,
 *  a title, buttons (any combination of OK, Cancel, Yes, and No) and an icon. 
 *  The Alert control is modal, which means it will retain focus until the user closes it.
 *
 *  <p>Import the mx.controls.Alert class into your application, 
 *  and then call the static <code>show()</code> method in ActionScript to display
 *  an Alert control. You cannot create an Alert control in MXML.</p>
 *
 *  <p>The Alert control closes when you select a button in the control, 
 *  or press the Escape key.</p>
 *
 *  @includeExample examples/SimpleAlert.mxml
 *
 *  @see mx.managers.SystemManager
 *  @see mx.managers.PopUpManager
 */
public class Alert extends Panel
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class initialization
    //
    //--------------------------------------------------------------------------
    
    loadResources();
    
    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

    /**
     *  Value that enables a Yes button on the Alert control when passed
     *  as the <code>flags</code> parameter of the <code>show()</code> method.
     *  You can use the | operator to combine this bitflag
     *  with the <code>OK</code>, <code>CANCEL</code>,
     *  <code>NO</code>, and <code>NONMODAL</code> flags.
     */
    public static const YES:uint = 0x0001;
    
    /**
     *  Value that enables a No button on the Alert control when passed
     *  as the <code>flags</code> parameter of the <code>show()</code> method.
     *  You can use the | operator to combine this bitflag
     *  with the <code>OK</code>, <code>CANCEL</code>,
     *  <code>YES</code>, and <code>NONMODAL</code> flags.
     */
    public static const NO:uint = 0x0002;
    
    /**
     *  Value that enables an OK button on the Alert control when passed
     *  as the <code>flags</code> parameter of the <code>show()</code> method.
     *  You can use the | operator to combine this bitflag
     *  with the <code>CANCEL</code>, <code>YES</code>,
     *  <code>NO</code>, and <code>NONMODAL</code> flags.
     */
    public static const OK:uint = 0x0004;
    
    /**
     *  Value that enables a Cancel button on the Alert control when passed
     *  as the <code>flags</code> parameter of the <code>show()</code> method.
     *  You can use the | operator to combine this bitflag
     *  with the <code>OK</code>, <code>YES</code>,
     *  <code>NO</code>, and <code>NONMODAL</code> flags.
     */
    public static const CANCEL:uint= 0x0008;

    /**
     *  Value that makes an Alert nonmodal when passed as the
     *  <code>flags</code> parameter of the <code>show()</code> method.
     *  You can use the | operator to combine this bitflag
     *  with the <code>OK</code>, <code>CANCEL</code>,
     *  <code>YES</code>, and <code>NO</code> flags.
     */
    public static const NONMODAL:uint = 0x8000;

    //--------------------------------------------------------------------------
    //
    //  Class mixins
    //
    //--------------------------------------------------------------------------
    
    /**
     *  @private
     *  Placeholder for mixin by AlertAccImpl.
     */
    mx_internal static var createAccessibilityImplementation:Function;

    //--------------------------------------------------------------------------
    //
    //  Class resources
    //
    //--------------------------------------------------------------------------

    [ResourceBundle("controls")]
    
    /**
     *  @private
     */ 
    private static var packageResources:ResourceBundle;

    /**
     *  @private
     */ 
    private static var resourceCancelLabel:String;  

    /**
     *  @private
     */ 
    private static var resourceNoLabel:String;

    /**
     *  @private
     */ 
    private static var resourceOkLabel:String;

    /**
     *  @private
     */ 
    private static var resourceYesLabel:String;

    //--------------------------------------------------------------------------
    //
    //  Class variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */ 
    private var init:Boolean = false;

    //--------------------------------------------------------------------------
    //
    //  Class properties
    //
    //--------------------------------------------------------------------------
    
    //----------------------------------
    //  buttonHeight
    //----------------------------------

    [Inspectable(category="Size")]

    /**
     *  Height of each Alert button, in pixels.
     *  All buttons must be the same height.
     *
     *  @default 22
     */
    public static var buttonHeight:Number = 22;
    
    //----------------------------------
    //  buttonWidth
    //----------------------------------

    [Inspectable(category="Size")]

    /**
     *  Width of each Alert button, in pixels.
     *  All buttons must be the same width.
     *
     *  @default 60
     */
    public static var buttonWidth:Number = 60;
    
    //----------------------------------
    //  cancelLabel
    //----------------------------------
    
    [Inspectable(category="General")]

    /**
     *  The label for the Cancel button.
     *
     *  <p>If you use a different label, you may need to adjust the 
     *  <code>buttonWidth</code> property to fully display it.</p>
     *
     *  The English resource bundle sets this property to "CANCEL". 
     */
    public static var cancelLabel:String = "";
    
    //----------------------------------
    //  noLabel
    //----------------------------------
    
    [Inspectable(category="General")]

    /**
     *  The label for the No button.
     *
     *  <p>If you use a different label, you may need to adjust the 
     *  <code>buttonWidth</code> property to fully display it.</p>
     *
     *  The English resource bundle sets this property to "NO". 
     */
    public static var noLabel:String = "";

    //----------------------------------
    //  okLabel
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  The label for the OK button.
     *
     *  <p>If you use a different label, you may need to adjust the 
     *  <code>buttonWidth</code> property to fully display the label.</p>
     *
     *  The English resource bundle sets this property to "OK". 
     */
    public static var okLabel:String = "";

    //----------------------------------
    //  yesLabel
    //----------------------------------
    
    [Inspectable(category="General")]

    /**
     *  The label for the Yes button.
     *
     *  <p>If you use a different label, you may need to adjust the 
     *  <code>buttonWidth</code> property to fully display the label.</p>
     *
     *  The English resource bundle sets this property to "YES". 
     */
    public static var yesLabel:String = "";

    //--------------------------------------------------------------------------
    //
    //  Class methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private    
     *  Loads resources for this class.
     */
    private static function loadResources():void
    {
        resourceCancelLabel = packageResources.getString("cancelLabel");

        resourceNoLabel = packageResources.getString("noLabel");

        resourceOkLabel = packageResources.getString("okLabel");

        resourceYesLabel = packageResources.getString("yesLabel");

        bundleChanged();
    }

    /**
     *  @private    
     *  Populates localizable properties from the loaded 
     *  bundle for this class.
     */
    private static function bundleChanged():void
    {
        okLabel = resourceOkLabel;
        yesLabel = resourceYesLabel;
        noLabel = resourceNoLabel;
        cancelLabel = resourceCancelLabel;
    }
    
    /**
     *  Static method that pops up the Alert control. The Alert control 
     *  closes when you select a button in the control, or press the Escape key.
     * 
     *  @param text Text string that appears in the Alert control. 
     *  This text is centered in the alert dialog box.
     *
     *  @param title Text string that appears in the title bar. 
     *  This text is left justified.
     *
     *  @param flags Which buttons to place in the Alert control.
     *  Valid values are <code>Alert.OK</code>, <code>Alert.CANCEL</code>,
     *  <code>Alert.YES</code>, and <code>Alert.NO</code>.
     *  The default value is <code>Alert.OK</code>.
     *  Use the bitwise OR operator to display more than one button. 
     *  For example, passing <code>(Alert.YES | Alert.NO)</code>
     *  displays Yes and No buttons.
     *  Regardless of the order that you specify buttons,
     *  they always appear in the following order from left to right:
     *  OK, Yes, No, Cancel.
     *
     *  @param parent Object upon which the Alert control centers itself.
     *
     *  @param closeHandler Event handler that is called when any button
     *  on the Alert control is pressed.
     *  The event object passed to this handler is an instance of CloseEvent;
     *  the <code>detail</code> property of this object contains the value
     *  <code>Alert.OK</code>, <code>Alert.CANCEL</code>,
     *  <code>Alert.YES</code>, or <code>Alert.NO</code>.
     *
     *  @param iconClass Class of the icon that is placed to the left
     *  of the text in the Alert control.
     *
     *  @param defaultButtonFlag A bitflag that specifies the default button.
     *  You can specify one and only one of
     *  <code>Alert.OK</code>, <code>Alert.CANCEL</code>,
     *  <code>Alert.YES</code>, or <code>Alert.NO</code>.
     *  The default value is <code>Alert.OK</code>.
     *  Pressing the Enter key triggers the default button
     *  just as if you clicked it. Pressing Escape triggers the Cancel
     *  or No button just as if you selected it.
     *
     *  @return A reference to the Alert control. 
     *
     *  @see mx.events.CloseEvent
     */
    public static function show(text:String = "", title:String = "",
                                flags:uint = 0x4 /* Alert.OK */, 
                                parent:Sprite = null, 
                                closeHandler:Function = null, 
                                iconClass:Class = null, 
                                defaultButtonFlag:uint = 0x4 /* Alert.OK */):Alert
    {
        var modal:Boolean = (flags & Alert.NONMODAL) ? false : true;

        if (!parent)
            parent = Sprite(Application.application);   
        
        var alert:Alert = new Alert();

        if (flags & Alert.OK||
            flags & Alert.CANCEL ||
            flags & Alert.YES ||
            flags & Alert.NO)
        {
            alert.buttonFlags = flags;
        }
        
        if (defaultButtonFlag == Alert.OK ||
            defaultButtonFlag == Alert.CANCEL ||
            defaultButtonFlag == Alert.YES ||
            defaultButtonFlag == Alert.NO)
        {
            alert.defaultButtonFlag = defaultButtonFlag;
        }
        
        alert.text = text;
        alert.title = title;
        alert.iconClass = iconClass;
            
        if (closeHandler != null)
            alert.addEventListener(CloseEvent.CLOSE, closeHandler);

        PopUpManager.addPopUp(alert, parent, modal);

        alert.setActualSize(alert.getExplicitOrMeasuredWidth(),
                            alert.getExplicitOrMeasuredHeight());
        
        return alert;
    }

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     */
    public function Alert()
    {
        super();

        // Panel properties.
        title = "";
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  alertForm
    //----------------------------------
    
    /**
     *  @private
     *  The internal AlertForm object that contains the text, icon, and buttons
     *  of the Alert control.
     */
    mx_internal var alertForm:AlertForm;

    //----------------------------------
    //  buttonFlags
    //----------------------------------

    /**
     *  A bitmask that contains <code>Alert.OK</code>, <code>Alert.CANCEL</code>, 
     *  <code>Alert.YES</code>, and/or <code>Alert.NO</code> indicating the buttons
     *  available in the Alert control.
     *
     *  @default Alert.OK
     */
    public var buttonFlags:uint = OK;
    
    //----------------------------------
    //  defaultButtonFlag
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  A bitflag that contains either <code>Alert.OK</code>, 
     *  <code>Alert.CANCEL</code>, <code>Alert.YES</code>, 
     *  or <code>Alert.NO</code> to specify the default button.
     *
     *  @default Alert.OK
     */
    public var defaultButtonFlag:uint = OK;
    
    //----------------------------------
    //  iconClass
    //----------------------------------

    [Inspectable(category="Other")]

    /**
     *  The class of the icon to display.
     *  You typically embed an asset, such as a JPEG or GIF file,
     *  and then use the variable associated with the embedded asset 
     *  to specify the value of this property.
     *
     *  @default null
     */
    public var iconClass:Class;
    
    //----------------------------------
    //  text
    //----------------------------------

    [Inspectable(category="General")]
    
    /**
     *  The text to display in this alert dialog box.
     *
     *  @default ""
     */
    public var text:String = "";
    
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function initializeAccessibility():void
    {
        if (Alert.createAccessibilityImplementation != null)
            Alert.createAccessibilityImplementation(this);
    }

    /**
     *  @private
     */
    override protected function createChildren():void
    {
        super.createChildren();

        var messageStyleName:String = getStyle("messageStyleName");
        if (messageStyleName)
            styleName = messageStyleName;

        if (!alertForm)
        {   
            alertForm = new AlertForm();
            alertForm.styleName = this;
            addChild(alertForm);
        }
    }

    /**
     *  @private
     */
    override protected function measure():void
    {   
        super.measure();
        
        var m:EdgeMetrics = viewMetrics;
        
        // The width is determined by the title or the AlertForm,
        // whichever is wider.
        measuredWidth = 
            Math.max(measuredWidth, alertForm.getExplicitOrMeasuredWidth() +
            m.left + m.right);
        
        measuredHeight = alertForm.getExplicitOrMeasuredHeight() +
                         m.top + m.bottom;
    }

    /**
     *  @private
     */
    override protected function updateDisplayList(unscaledWidth:Number,
                                                  unscaledHeight:Number):void
    {
        super.updateDisplayList(unscaledWidth, unscaledHeight);
        
        // Position the AlertForm inside the "client area" of the Panel
        var vm:EdgeMetrics = viewMetrics;
        alertForm.setActualSize(unscaledWidth - vm.left - vm.right -
                                getStyle("paddingLeft") -
                                getStyle("paddingRight"),
                                unscaledHeight - vm.top - vm.bottom -
                                getStyle("paddingTop") -
                                getStyle("paddingBottom"));

        // Choose an (x,y) position that centers me in my parent.       
        if (!init)
        {
            var x:Number;
            var y:Number;
            if (parent == systemManager)
            {
                x = (screen.width - measuredWidth) / 2;
                y = (screen.height - measuredHeight) / 2;
            }
            else
            {
                x = (parent.width - measuredWidth) / 2;
                y = (parent.height - measuredHeight) / 2;
            }

            // Set my position, because my parent won't do it for me.
            move(Math.round(x), Math.round(y));
            init = true;
        }
    }

    /**
     *  @private
     */
    override public function styleChanged(styleProp:String):void
    {
        super.styleChanged(styleProp);
        
        if (styleProp == "messageStyleName")
        {
            var messageStyleName:String =
                getStyle("messageStyleName");

            styleName = messageStyleName;
        }
        
        if (alertForm)
            alertForm.styleChanged(styleProp);
    }

}

}
