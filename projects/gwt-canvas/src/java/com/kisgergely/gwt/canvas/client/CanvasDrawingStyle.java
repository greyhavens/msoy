package com.kisgergely.gwt.canvas.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wrapper class for Canvas drawing styles. 3 types of styles are supported by the class:
 * COLOR -    the string representation of a CSS Color element
 * PATTERN -  pattern as created by CanvasRenderingContext2D.createPattern()
 * GRADIENT - gradient as created by CanvasRenderingContext2D.createLinearGradient() 
 *            or createRadialGradient()
 * This class was created to provide a cleaner, type-safer way to access canvas drawing styles.           
 * @author Gergely Kis (kisg@kisgergely.com)
 *
 */
public class CanvasDrawingStyle {

	/**
	 * Default type value
	 */
	public static final short UNDEFINED = 0;
	
	/**
	 * Type value to mark color drawing styles.
	 */
	public static final short COLOR     = 1;
	
	/**
	 * Type value to mark pattern drawing styles.
	 */
	public static final short PATTERN   = 2;
	
	/**
	 * Type value to mark gradient drawing styles.
	 */
	public static final short GRADIENT  = 3;
	
	/**
	 * Field to select between the different drawing style types
	 */
	private short styleType = UNDEFINED;
	
	/**
	 * The wrapped JS object in case of pattern and gradient styles
	 */
	private JavaScriptObject drawingStyle = null;
	
	/**
	 * The CSS color string representation in case of color styles
	 */
	private String color = null;
	
	/**
	 * Constructs a gradient or pattern style
	 * This constructor may only be called by the appropriate methods of the 
	 * CanvasRenderingContext2D class.
	 * @param styleType GRADIENT or PATTERN to designate the style
	 * @param drawingStyle The JS Object to wrap
	 */
	protected CanvasDrawingStyle(short styleType, JavaScriptObject drawingStyle) {
		if (styleType != PATTERN && styleType != GRADIENT)
			throw new IllegalArgumentException("Only PATTERN or GRADIENT is allowed as styleType");
		this.styleType = styleType;
		this.drawingStyle = drawingStyle;
	}

	/**
	 * Constructs a color style from a cssColor string.
	 * @param cssColor the string representing the CSS color
	 */
	public CanvasDrawingStyle(String cssColor)
	{
		if (cssColor == null)
		{
			throw new IllegalArgumentException("cssColor should never be null");
		}
		styleType = COLOR;
		color = cssColor;
	}
	
	/**
	 * Gets the string representation of the color style or returns null otherwise.
	 * @return String or null
	 */
	public String getColorString()
	{
		if (styleType == COLOR)
		{
			return color;
		}
		return null;
	}
	
	/**
	 * Gets the type of the style
	 * @return COLOR, PATTERN or GRADIENT
	 */
	public short getStyleType()
	{
		return styleType;
	}
	
	/**
	 * Checks whether the instance in the parameter equals to this instance.
	 * @param o Object to compare
	 */
	public boolean equals(Object o)
	{
		if (o.getClass().getName().equals(getClass().getName())) {
			return equals((CanvasDrawingStyle) o);
		}
		return false;
	}
	
	/**
	 * Compares a CanvasDrawingStyle instance explicitly.
	 * A Color style is equal to an other style if their string representation equals
	 * to eachother.
	 * A Pattern or Gradient style equals to another if their wrapped JS objects equal. 
	 * @param style the style to compare
	 * @return true if the two styles are equal, false otherwise
	 */
	public boolean equals(CanvasDrawingStyle style)
	{
		if (style == null)
			return false;
		if (styleType == COLOR)
			return color.equals(style.getColorString());
		return (drawingStyle.equals(style.drawingStyle));
	}
	
	/**
	 * Adds stop colors at specific offsets for gradient styles.
	 * This method is the direct wrapper of the CanvasGradient.addStopColor JS method.
	 * @param offset The offset to set the stop color
	 * @param cssColor The stop color
	 */
	public void addColorStop(float offset, String cssColor) {
		if (styleType != GRADIENT)
			throw new UnsupportedOperationException("This operation is only supported for GRADIENT styles");
		addColorStop(drawingStyle, offset, cssColor);
	}

	/**
	 * JSNI wrapper method for CanvasGradient.addColorStop()
	 * @param drawingStyle wrapped gradient object
	 * @param offset 
	 * @param cssColor
	 */
	private native void addColorStop(JavaScriptObject drawingStyle, float offset, String cssColor) /*-{
		drawingStyle.addColorStop(offset, cssColor);		
	}-*/;

	/**
	 * Returns the wrapped JS object. Used by the CanvasRenderingContext2D wrapper to access
	 * the wrapped gradient and pattern objects 
	 * @return JS object
	 */
	protected JavaScriptObject getJsDrawingStyle()
	{
		return drawingStyle;
	}
}
