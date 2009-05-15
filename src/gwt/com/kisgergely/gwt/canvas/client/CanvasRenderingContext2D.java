package com.kisgergely.gwt.canvas.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Image;

/**
 * Wrapper Class to represent the 2D Rendering Context of a Canvas element.
 * It implements all the methods of the WhatWG specification but with some
 * adaptations to better align with the Java language.
 * @author Gergely Kis (kisg@kisgergely.com)
 *
 */
public class CanvasRenderingContext2D {

	/**
	 * The wrapped JS context object;
	 */
	private JavaScriptObject jsCtx = null;
	
	/**
	 * The parent Canvas widget.
	 */
	private Canvas canvas = null;

	private CanvasDrawingStyle strokeStyle = null;
	
	private CanvasDrawingStyle fillStyle = null;
	
	/**
	 * Constructs a rendering context instance. This method is only called
	 * by the Canvas class. 
	 * @param canvas the parent canvas for this context
	 * @param jsCtx  the wrapped JS context object
	 */
	protected CanvasRenderingContext2D(Canvas canvas, JavaScriptObject jsCtx) {
		this.canvas = canvas;
		this.jsCtx = jsCtx;
	}

	/**
	 * Saves the current Drawing State to the state stack
	 */
	public native void save() /*-{ 
		var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
		ctx.save();
	}-*/;
	
	/**
	 * Restores the previous Drawing State from the state stack
	 */
	public native void restore() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.restore();
	}-*/;
	
	public native void scale(float x, float y) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.scale(x, y);	
	}-*/;
	
	public native void rotate(float angle)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.rotate(angle);	
	}-*/;

	public native void translate(float x, float y) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.translate(x, y);	
	}-*/;
	
	public native void transform(float m11, float m12, float m21, float m22, float dx, float dy) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.transform(m11, m12, m21, m22, dx, dy);	
	}-*/;
	
	public native void setTransform(float m11, float m12, float m21, float m22, float dx, float dy) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.setTransform(m11, m12, m21, m22, dx, dy);	
	}-*/;
	
	public native float getGlobalAlpha() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.globalAlpha;
	}-*/;
	
	public native void setGlobalAlpha(float alpha) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.globalAlpha = alpha;
	}-*/;

	public native String getGlobalCompositeOperation() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.globalCompositeOperation;
	}-*/;
	
	public native void setGlobalCompositeOperation(String operation) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.globalCompositeOperation = operation;
	}-*/;
	
	private native String getStrokeStyleImplString() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.strokeStyle;
	}-*/;
	
	private native JavaScriptObject getStrokeStyleImplObject() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.strokeStyle;
	}-*/;

	private native short getStrokeStyleImplType() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	if (ctx.strokeStyle instanceof String)
	{
		return @com.kisgergely.gwt.canvas.client.CanvasDrawingStyle::COLOR;
	} 
	else if (ctx.strokeStyle instanceof CanvasGradient)
	{
		return @com.kisgergely.gwt.canvas.client.CanvasDrawingStyle::GRADIENT;
	}
	else if (ctx.strokeStyle instanceof CanvasPattern)
	{
		return @com.kisgergely.gwt.canvas.client.CanvasDrawingStyle::PATTERN;
	}
	else
	{
		return @com.kisgergely.gwt.canvas.client.CanvasDrawingStyle::UNDEFINED;
	}
	}-*/;
	
	private native void setStrokeStyleImpl(String style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.strokeStyle = style;
	}-*/;
	
	private native void setStrokeStyleImpl(JavaScriptObject style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.strokeStyle = style;
	}-*/;
	
	public void setStrokeStyle(String cssColor)
	{
		if (strokeStyle == null || strokeStyle.getStyleType() != CanvasDrawingStyle.COLOR
				|| !strokeStyle.getColorString().equals(cssColor));
		{
			strokeStyle = new CanvasDrawingStyle(cssColor);
		}
		setStrokeStyleImpl(cssColor);
	}

	public void setStrokeStyle(CanvasDrawingStyle style)
	{
		if (style != null)
		{
			strokeStyle = style;
			if (style.getStyleType() == CanvasDrawingStyle.COLOR)
			{
				setStrokeStyleImpl(style.getColorString());
			} else
			{
				setStrokeStyleImpl(style.getJsDrawingStyle());
			}
		}
	}
	
	public CanvasDrawingStyle getStrokeStyle()
	{
		short styleType = getStrokeStyleImplType();
		CanvasDrawingStyle currStyle = null;
		if (styleType == CanvasDrawingStyle.COLOR)
		{
			currStyle = new CanvasDrawingStyle(getStrokeStyleImplString());
		} else
		{
			currStyle = new CanvasDrawingStyle(styleType, getStrokeStyleImplObject());
		}
		if (!currStyle.equals(strokeStyle))
		{
			strokeStyle = currStyle;
		}
		return strokeStyle;
	}
	
	
	private native String getFillStyleImplString() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.fillStyle;
	}-*/;
	
	private native JavaScriptObject getFillStyleImplObject() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.fillStyle;
	}-*/;

	private native short getFillStyleImplType() /*-{  
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	{
		return @com.kisgergely.gwt.canvas.client.CanvasDrawingStyle::UNDEFINED;
	}
	}-*/;
	
	private native void setFillStyleImpl(String style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.fillStyle = style;
	}-*/;
	
	private native void setFillStyleImpl(JavaScriptObject style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.fillStyle = style;
	}-*/;
	
	public void setFillStyle(String cssColor)
	{
		if (fillStyle == null 
				|| fillStyle.getStyleType() != CanvasDrawingStyle.COLOR
				|| !fillStyle.getColorString().equals(cssColor))
		{
			fillStyle = new CanvasDrawingStyle(cssColor);
		}
		setFillStyleImpl(cssColor);
	}

	public void setFillStyle(CanvasDrawingStyle style)
	{
		if (style != null)
		{
			fillStyle = style;
			if (style.getStyleType() == CanvasDrawingStyle.COLOR)
			{
				setFillStyleImpl(style.getColorString());
			} else
			{
				setFillStyleImpl(style.getJsDrawingStyle());
			}
		}
	}
	
	public CanvasDrawingStyle getFillStyle()
	{
		short styleType = getFillStyleImplType();
		CanvasDrawingStyle currStyle = null;
		if (styleType == CanvasDrawingStyle.COLOR)
		{
			currStyle = new CanvasDrawingStyle(getFillStyleImplString());
		} else
		{
			currStyle = new CanvasDrawingStyle(styleType, getFillStyleImplObject());
		}
		if (!currStyle.equals(fillStyle))
		{
			fillStyle = currStyle;
		}
		return fillStyle;
	}
	

	private native JavaScriptObject createLinearGradientImpl(float x0, float y0, float x1, float y1) /*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.createLinearGradient(x0, y0, x1, y1);	
	}-*/;

	private native JavaScriptObject createRadialGradientImpl(float x0, float y0, float r0, float x1, float y1, float r1) /*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.createRadialGradient(x0, y0, r0, x1, y1, r1);	
	}-*/;
	
	private native JavaScriptObject createPatternImpl(Element elem, String repetition)  /*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	if (ctx.createPattern)
		return ctx.createPattern(elem, repetition);
	return null;	
	}-*/; 
	
	public CanvasDrawingStyle createLinearGradient(float x0, float y0, float x1, float y1)
	{
		return new CanvasDrawingStyle(CanvasDrawingStyle.GRADIENT, 
				createLinearGradientImpl(x0,y0,x1,y1));
	}
	
	public CanvasDrawingStyle createRadialGradient(float x0, float y0, float r0, float x1, float y1, float r1)
	{
		return new CanvasDrawingStyle(CanvasDrawingStyle.GRADIENT, 
				createRadialGradientImpl(x0,y0,r0,x1,y1,r1));
	}

	public CanvasDrawingStyle createPattern(Image img, String repetition)
	{
		return new CanvasDrawingStyle(CanvasDrawingStyle.PATTERN, 
				createPatternImpl(img.getElement(),repetition));
	}

	public CanvasDrawingStyle createPattern(Canvas canvas, String repetition)
	{
		return new CanvasDrawingStyle(CanvasDrawingStyle.PATTERN, 
				createPatternImpl(canvas.getElement(),repetition));
	}
	  
	public native float getLineWidth() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.lineWidth;
	}-*/;
	
	public native void setLineWidth(float lw) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.lineWidth = lw;
	}-*/;

	/**
	 * 
	 * @return "butt", "round", "square" (default "butt")
	 */
	public native String getLineCap() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.lineCap;
	}-*/;
	
	public native void setLineCap(String style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.lineCap = style;
	}-*/;

	/**
	 * 
	 * @return "round", "bevel", "miter" (default "miter")
	 */
	public native String getLineJoin() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.lineJoin;
	}-*/;
	
	public native void setLineJoin(String style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.lineJoin = style;
	}-*/;

	public native float getMiterLimit() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.miterLimit;
	}-*/;
	
	public native void setMiterLimit(float ml) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.miterLimit = ml;
	}-*/;
	
	public native float getShadowOffsetX() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.shadowOffsetX;
	}-*/;
	
	public native void setShadowOffsetX(float x) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.shadowOffsetX = x;
	}-*/;

	public native float getShadowOffsetY() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.shadowOffsetY;
	}-*/;
	
	public native void setShadowOffsetY(float y) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.shadowOffsetY = y;
	}-*/;
	
	public native float getShadowBlur() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.shadowBlur;
	}-*/;
	
	public native void setShadowBlur(float blur) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.shadowBlur = blur;
	}-*/;
	
	public native String getShadowColor() /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.shadowColor;
	}-*/;
	
	public native void setShadowColor(String style) /*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.shadowColor = style;
	}-*/;

	public native void clearRect(float x, float y, float w, float h)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.clearRect(x, y, w, h);
	}-*/;
	
	public native void fillRect(float x, float y, float w, float h)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.fillRect(x, y, w, h)
	}-*/;
	
	public native void strokeRect(float x, float y, float w, float h)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.strokeRect(x, y, w, h)
	}-*/;

	public native void beginPath()/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.beginPath();
	}-*/;
	
	public native void closePath()/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.closePath();
	}-*/;

	
	public native void moveTo(float x, float y)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.moveTo(x,y);
	}-*/;
	
	public native void lineTo(float x, float y)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.lineTo(x,y);
	}-*/;
	
	public native void quadraticCurveTo(float cpx, float cpy, float x, float y)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.quadraticCurveTo(cpx, cpy, x, y);
	}-*/;
	
	public native void bezierCurveTo(float cp1x, float cp1y, float cp2x, float cp2y, float x, float y)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
	}-*/;
	
	public native void arcTo(float x1, float y1, float x2, float y2, float radius)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.arcTo(x1, y1, x2, y2, radius);
	}-*/;
	
	public native void rect(float x, float y, float w, float h)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.rect(x, y, w, h);
	}-*/;
	
	public native void arc(float x, float y, float radius, float startAngle, float endAngle, boolean anticlockwise)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.arc(x, y, radius, startAngle, endAngle, anticlockwise);
	}-*/;
	
	public native void fill()/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.fill();
	}-*/;
	
	public native void stroke()/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.stroke();
	}-*/;

	public native void clip()/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.clip();
	}-*/;
	
	public native boolean isPointInPath(float x, float y)/*-{ 
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.isPointInPath(x, y);
	}-*/;

	private native void drawImageImpl(Element elem, float dx, float dy) /*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.drawImage(elem, dx, dy);	
	}-*/ ;
	
	private native void drawImageImpl(Element elem, float dx, float dy, float dw, float dh) /*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.drawImage(elem, dx, dy, dw, dh);	
	}-*/ ;
	
	private native void drawImageImpl(Element elem, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh)/*-{
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	ctx.drawImage(elem, sx, sy, sw, sh, dx, dy, dw, dh);	
	}-*/ ;

	public void drawImage(Image img, float dx, float dy)
	{
		drawImageImpl(img.getElement(), dx, dy);
	}
	
	public void drawImage(Image img, float dx, float dy, float dw, float dh)
	{
		drawImageImpl(img.getElement(), dx, dy, dw, dh);
	}

	public void drawImage(Image img, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh)
	{
		drawImageImpl(img.getElement(), sx, sy, sw, sh, dx, dy, dw, dh);
	}
	
	public void drawImage(Canvas cnv, float dx, float dy)
	{
		drawImageImpl(cnv.getElement(), dx, dy);
	}
	
	public void drawImage(Canvas cnv, float dx, float dy, float dw, float dh)
	{
		drawImageImpl(cnv.getElement(), dx, dy, dw, dh);
	}

	public void drawImage(Canvas cnv, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh)
	{
		drawImageImpl(cnv.getElement(), sx, sy, sw, sh, dx, dy, dw, dh);
	}

	private native JavaScriptObject getImageDataImpl(float sx, float sy, float sw, float sh) /*-{ 	
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.getImageData(sx,sy,sw,sh);	
	}-*/ ;

	private native void putImageDataImpl(JavaScriptObject imgdata, float dx, float dy) /*-{ 	
	var ctx = this.@com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D::jsCtx;
	return ctx.putImageData(imgdata, dx,dy);	
	}-*/ ;
	
	public CanvasImageData getImageData(float sx, float sy, float sw, float sh) {
		return new CanvasImageData(getImageDataImpl(sx,sy,sw,sh));
	}
	
	public void putImageData(CanvasImageData imgdata, float dx, float dy)
	{
		putImageDataImpl(imgdata.getJsImageData(), dx, dy);
	}
	
	protected JavaScriptObject getJsContext() {
		return jsCtx;
	}

	public Canvas getCanvas() {
		return canvas;
	}

}
