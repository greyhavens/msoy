package com.kisgergely.gwt.canvas.client;

import com.google.gwt.core.client.JavaScriptObject;

public class CanvasImageData {

	JavaScriptObject jsImageData = null;
	
	protected CanvasImageData(JavaScriptObject jsImageData) {
		this.jsImageData = jsImageData;
	}
	
	public native int getWidth() /*-{
		var imgdata = this.@com.kisgergely.gwt.canvas.client.CanvasImageData::jsImageData;
		return imgdata.width;
	}-*/;

	public native int getHeight() /*-{
	var imgdata = this.@com.kisgergely.gwt.canvas.client.CanvasImageData::jsImageData;
	return imgdata.height;
	}-*/;

	public JavaScriptObject getJsImageData() {
		return jsImageData;
	}


}
