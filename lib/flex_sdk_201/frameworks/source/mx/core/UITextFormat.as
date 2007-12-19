////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextLineMetrics;
import mx.managers.ISystemManager;

/**
 *  The UITextFormat class represents character formatting information
 *  for the UITextField class.
 *  The UITextField class defines the component used by many Flex composite
 *  components to display text.
 *
 *  <p>The UITextFormat class extends the Flash Player's TextFormat class
 *  to add the text measurement methods <code>measureText()</code>
 *  and <code>measureHTMLText()</code> and to add properties for
 *  controlling the FlashType font renderer.</p>
 *
 *  @see mx.core.UITextField
 */
public class UITextFormat extends TextFormat
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class variables
    //
    //--------------------------------------------------------------------------

	/**
	 *  @private
	 *  The measureText() method uses this single TextField to perform
	 *  text measurement..
	 */
	private static var measurementTextField:TextField;

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *
     * 	@param systemManager A SystemManager object.
	 *  The SystemManager keeps track of which fonts are embedded.
	 *  Typically this is the SystemManager obtained from the
	 *  <code>systemManager</code> property of UIComponent.
	 *
     * 	@param font A String specifying the name of a font,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param size A Number specifying a font size in pixels,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param color An unsigned integer specifying the RGB color of the text,
	 *  such as 0xFF0000 for red, or <code>null</code> to indicate
	 *  that is UITextFormat doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param bold A Boolean flag specifying whether the text is bold,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param italic A Boolean flag specifying whether the text is italic,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param italic A Boolean flag specifying whether the text is underlined,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param urlString A String specifying the URL to which the text is
	 *  hyperlinked, or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param target A String specifying the target window
	 *  where the hyperlinked URL is displayed. 
	 *  If the target window is <code>null</code> or an empty string,
	 *  the hyperlinked page is displayed in the same browser window.
	 *  If the <code>urlString</code> parameter is <code>null</code>
	 *  or an empty string, this property has no effect.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param align A String specifying the alignment of the paragraph,
	 *  as a flash.text.TextFormatAlign value, or <code>null</code> to indicate
	 *  that this UITextFormat doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param leftMargin A Number specifying the left margin of the paragraph,
	 *  in pixels, or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param rightMargin A Number specifying the right margin of the paragraph,
	 *  in pixels, or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param indent A Number specifying the indentation from the left
	 *  margin to the first character in the paragraph, in pixels,
	 *  or <code>null</code> to indicate that this UITextFormat
	 *  doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 * 	@param leading A Number specifying the amount of additional vertical
	 *  space between lines, or <code>null</code> to indicate
	 *  that this UITextFormat doesn't specify this property.
	 *  This parameter is optional, with a default value of <code>null</code>.
	 *
	 *  @see flash.text.TextFormatAlign
     */
    public function UITextFormat(systemManager:ISystemManager,
								 font:String = null,
								 size:Object = null,
								 color:Object = null,
                    			 bold:Object = null,
								 italic:Object = null,
								 underline:Object = null,
                    			 url:String = null,
								 target:String = null,
								 align:String = null,
                    			 leftMargin:Object = null,
								 rightMargin:Object = null,
                    			 indent:Object = null,
								 leading:Object = null)
    {
		this.systemManager = systemManager;

        super(font, size, color, bold, italic, underline, url, target,
			  align, leftMargin, rightMargin, indent, leading);
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	private var systemManager:ISystemManager;

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
	//  antiAliasType
    //----------------------------------

    /**
     *  Defines the anti-aliasing setting for the UITextField class.
     *  The possible values are <code>"normal"</code> 
     *  (<code>flash.text.AntiAliasType.NORMAL</code>) 
     *  and <code>"advanced"</code> 
     *  (<code>flash.text.AntiAliasType.ADVANCED</code>). 
	 *  
	 *  <p>The default value is <code>"advanced"</code>, 
	 *  which enables the FlashType renderer if you are using an 
	 *  embedded FlashType font. 
	 *  Set this property to <code>"normal"</code>
	 *  to disable the FlashType renderer.</p>
	 *  
	 *  <p>This property has no effect for system fonts.</p>
	 *  
	 *  <p>This property applies to all the text in a UITextField object; 
	 *  you cannot apply it to some characters and not others.</p>
	 * 
	 *  @default "advanced"
	 *
	 *  @see flash.text.AntiAliasType
	 */
	public var antiAliasType:String;
	
    //----------------------------------
	//  gridFitType
    //----------------------------------

    /**
     *  Defines the grid-fitting setting for the UITextField class.
	 *  The possible values are <code>"none"</code> 
	 *  (<code>flash.text.GridFitType.NONE</code>), 
	 *  <code>"pixel"</code> 
	 *  (<code>flash.text.GridFitType.PIXEL</code>),
	 *  and <code>"subpixel"</code> 
	 *  (<code>flash.text.GridFitType.SUBPIXEL</code>). 
	 *  
	 *  <p>This property only applies when you are using an
	 *  embedded FlashType font and the <code>fontAntiAliasType</code>
	 *  property is set to <code>"advanced"</code>.</p>
	 *  
	 *  <p>This property has no effect for system fonts.</p>
	 * 
	 *  <p>This property applies to all the text in a UITextField object; 
	 *  you cannot apply it to some characters and not others.</p>
	 * 
	 *  @default "pixel"
	 *
	 *  @see flash.text.GridFitType
	 */
	public var gridFitType:String;
	
    //----------------------------------
	//  sharpness
    //----------------------------------

    /**
     *  Defines the sharpness setting for the UITextField class.
	 *  This property specifies the sharpness of the glyph edges. 
	 *  The possible values are Numbers from -400 through 400. 
	 *  
	 *  <p>This property only applies when you are using an 
	 *  embedded FlashType font and the <code>fontAntiAliasType</code>
	 *  property is set to <code>"advanced"</code>.</p>
	 *  
	 *  <p>This property has no effect for system fonts.</p>
	 * 
	 *  <p>This property applies to all the text in a UITextField object; 
	 *  you cannot apply it to some characters and not others.</p>
	 *  
	 *  @default 0
	 *  @see flash.text.TextField
	 */
	public var sharpness:Number;
	
    //----------------------------------
	//  thickness
    //----------------------------------

    /**
     *  Defines the thickness setting for the UITextField class.
	 *  This property specifies the thickness of the glyph edges.
	 *  The possible values are Numbers from -200 to 200. 
	 *  
	 *  <p>This property only applies when you are using an 
	 *  embedded FlashType font and the <code>fontAntiAliasType</code>
	 *  property is set to <code>"advanced"</code>.</p>
	 *  
	 *  <p>This property has no effect for system fonts.</p>
	 * 
	 *  <p>This property applies to all the text in a UITextField object; 
	 *  you cannot apply it to some characters and not others.</p>
	 *  
	 *  @default 0
	 *  @see flash.text.TextField
	 */
	public var thickness:Number;
	
	//--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

	/**
	 *  Returns measurement information for the specified text, 
	 *  assuming that it is displayed in a single-line UITextField component, 
	 *  and using this UITextFormat object to define the text format. 
	 *
	 *  @param text A String specifying the text to measure.
	 *  
	 *  @return A TextLineMetrics object containing the text measurements.
	 *
	 *  @see flash.text.TextLineMetrics
	 */
	public function measureText(text:String):TextLineMetrics
	{
		return measure(text, false);
	}

	/**
	 *  Returns measurement information for the specified HTML text, 
	 *  which may contain HTML tags such as <code>&lt;font&gt;</code>
	 *  and <code>&lt;b&gt;</code>, assuming that it is displayed
	 *  in a single-line UITextField, and using this UITextFormat object
	 *  to define the text format.
	 *
	 *  @param text A String specifying the HTML text to measure.
	 *  
	 *  @return A TextLineMetrics object containing the text measurements.
	 *
	 *  @see flash.text.TextLineMetrics
	 */
	public function measureHTMLText(htmlText:String):TextLineMetrics
	{
		return measure(htmlText, true);
	}

	/**
	 *  @private
	 */
	private function measure(s:String, html:Boolean):TextLineMetrics
	{
		// The text of a TextField can't be set to null.
		if (!s)
			s = "";
		
		// Create a single, persistent, off-display-list TextField
		// to be used for text measurement.
		if (!measurementTextField)
			measurementTextField = new TextField();
				
		// Clear any old text from the TextField.
		// Otherwise, new text will get the old TextFormat. 
		if (html)
			measurementTextField.htmlText = "";
		else
			measurementTextField.text = "";

		// Make the measurement TextField use this TextFormat.
		measurementTextField.defaultTextFormat = this;
		var sm:ISystemManager = systemManager;
		if (font)
		{
			measurementTextField.embedFonts =
				sm != null && sm.isFontFaceEmbedded(this);
		}
		else
		{
			measurementTextField.embedFonts = false;
		}

		// Set other TextField properties based on CSS styles.
		measurementTextField.antiAliasType = antiAliasType;
		measurementTextField.gridFitType = gridFitType;
		measurementTextField.sharpness = sharpness;
		measurementTextField.thickness = thickness;
		
		// Set the text to be measured into the TextField.
		if (html)
			measurementTextField.htmlText = s;
		else
			measurementTextField.text = s;
		
		// Measure it.
		var lineMetrics:TextLineMetrics =
			measurementTextField.getLineMetrics(0);
								
		lineMetrics.width = Math.ceil(lineMetrics.width);
		lineMetrics.height = Math.ceil(lineMetrics.height);
		// Round up because embedded fonts can produce fractional values;
		// if a parent container rounds a component's actual width or height
		// down, the component may not be wide enough to display the text.
		
		return lineMetrics;
	}

    /**
     *  @private
	 */
	mx_internal function copyFrom(source:TextFormat):void
	{
		font = source.font;
		size = source.size;
		color = source.color;
		bold = source.bold;
		italic = source.italic;
		underline = source.underline;
		url = source.url;
		target = source.target;
		align = source.align;
		leftMargin = source.leftMargin;
		rightMargin = source.rightMargin;
		indent = source.indent;
		leading = source.leading;
	}
}

}
