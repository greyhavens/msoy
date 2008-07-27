package com.kisgergely.gwt.canvas.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseListenerCollection;
import com.google.gwt.user.client.ui.SourcesMouseEvents;

/**
 * Wrapper class for the Canvas HTML element
 * It implements every method of the WhatWG specification.
 * @author Gergely Kis (kisg@kisgergely.com)
 *
 */
public class Canvas extends FocusWidget implements SourcesMouseEvents {

	private static final String DEFAULT_STYLE_NAME = "gwt-Canvas"; 
	/**
	 * The 2D Context for this Canvas instance
	 */
	CanvasRenderingContext2D context = null;

	 private MouseListenerCollection mouseListeners = new MouseListenerCollection(); 
	
	/**
	 * Public constructor to create the Canvas element.
	 * The API was modified so it makes better use of the static-typedness 
	 * of Java: instead of using getContext(String) the getContext2D method
	 * is provided to return a CanvasRenderingContext2D instance without the 
	 * need for casting.
	 * @param width  Canvas width
	 * @param height Canvas height
	 */
	public Canvas(int width, int height)
	{
		super(DOM.createElement("canvas"));
		if (getElement() == null) throw new RuntimeException("Canvas could not be created");
		DOM.setElementProperty(getElement(),"width",new Integer(width).toString());
		DOM.setElementProperty(getElement(),"height",new Integer(height).toString());
		setStyleName(DEFAULT_STYLE_NAME);
		sinkEvents(Event.MOUSEEVENTS);

		if (isVmlCanvasManagerPresent())
		{
			 Element element = 
				 	 DOM.createElement("<canvas width='" + width +
					 "' height='" + height + "'></canvas>");
		     initElement(element); 		
		}

	
	}
	
	/** 
	 * 	Returns the 2D Canvas rendering context. 
	 * @return the reference to the rendering context or null if the 2D 
	 * rendering context is not available.
	 */
	public CanvasRenderingContext2D getContext2D()
	{
		if (context != null)
			return context;
		
		JavaScriptObject jscriptContext = getContext(getElement(),"2d");
		if (jscriptContext != null) {
			context = new CanvasRenderingContext2D(this, jscriptContext);
			return context;
		}
		
		return null;
	}

	/**
	 * Method to return a data URL to the image/png representation of the Canvas data
	 * @return the String with the data URL
	 */
	public native String toDataURL() /*-{ 
	var canvas = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
	if (canvas.toDataURL)
		return canvas.toDataURL();
	return null;
	}-*/;

	/**
	 * Method to return a data URL to the image representation of the Canvas content in the
	 * specified format. The standard only requires the support of image/png, if the 
	 * requested format is not supported then the implementation returns the data URL in PNG format.
	 * The caller should check whether the prefix of the returned URL equals to 
	 * data:image/png or to the requested MIME type.
	 * @param type MIME type of the requested data URL, e.g. image/gif
	 * @return the String with the data URL
	 */
	public native String toDataURL(String type) /*-{ 
	var canvas = this.@com.google.gwt.user.client.ui.UIObject::getElement()();
	if (canvas.toDataURL)
		return canvas.toDataURL(type);
	return null;	
	}-*/;

	/**
	 * Private JSNI method to get the JavaScriptObject of the Context
	 * @param element HTML Element of the Canvas
	 * @param ctx Context selector string
	 * @return the JavaScriptObject of the requested Context, or null if the Context
	 * is not available
	 */
	private native JavaScriptObject getContext(Element element, String ctx) /*-{
	if (element.getContext)
	{
		return element.getContext(ctx);
	} else {
		return null;
	}		
}-*/;

	public void addMouseListener(MouseListener listener) {
		mouseListeners.add(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		mouseListeners.remove(listener);		
	}

	public void onBrowserEvent(Event event) {
		super.onBrowserEvent(event);
        switch (DOM.eventGetType(event)) {
        case Event.ONMOUSEDOWN:
        case Event.ONMOUSEMOVE:
        case Event.ONMOUSEOUT:
        case Event.ONMOUSEOVER:
        case Event.ONMOUSEUP:
                mouseListeners.fireMouseEvent(this, event);
                break;
        }
    }

	private native boolean isVmlCanvasManagerPresent() /*-{
	    return typeof $wnd.G_vmlCanvasManager == "object";
	}-*/;

	private native void initElement(Element element) /*-{
	    var div = $wnd.document.createElement("div");
	    // kludge, so that initElement() won't
	    // complain about parentNode being null
	    div.appendChild(element); 
	    // 	newest excanvas.js, initElement() returns the fixed element
	    var newElement = $wnd.G_vmlCanvasManager.initElement(element); 
	    div.removeChild(newElement);
	    this.@com.google.gwt.user.client.ui.UIObject::setElement(Lcom/google/gwt/user/client/Element;)(newElement);
	}-*/;
	
}	
