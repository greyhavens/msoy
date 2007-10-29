package com.kisgergely.gwt.canvas.test;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasDrawingStyle;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;

public class CanvasMouseTest extends ClientTestCase {

	public CanvasMouseTest() {
		super("Canvas Mouse Test");
	}

	public void doTest(ClientTestCaseContext testCtx) {
		Canvas c = new Canvas(350,350);
		log("Canvas created");
		// The canvas should be there
		assertNotNull(c, "Canvas");
				
	    CanvasRenderingContext2D ctx = c.getContext2D();
	    log("Context acquired");
	    
	    assertNotNull(ctx, "Ctx2d");

	    if (ctx != null)
	    {
	    		final VerticalPanel vp = new VerticalPanel();
	    		final Label eventInfo = new Label();
	    		eventInfo.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
	    		
	    		vp.add(c);
	    		vp.add(eventInfo);
	    		
	    		log("Setting output widget");
	    		assertNotNull(testCtx.getOutputPanel(),"GetOutputPanel");
	    		testCtx.getOutputPanel().add(vp);
	    		
	    		// This is necessary because otherwise the Canvas
	    		// is not always displayed in Firefox 1.5
	    		testCtx.getOutputPanel().setSize(""+c.getOffsetWidth()+100,""+c.getOffsetHeight()+100);
	    		
	    		c.addMouseListener(new MouseListener() {

					public void onMouseDown(Widget sender, int x, int y) {
						eventInfo.setText("onMouseDown: "+x+", "+y);
					}

					public void onMouseEnter(Widget sender) {
						eventInfo.setText("onMouseEnter");
					}

					public void onMouseLeave(Widget sender) {
						eventInfo.setText("onMouseLeave");
					}

					public void onMouseMove(Widget sender, int x, int y) {
						eventInfo.setText("onMouseMove: "+x+", "+y);
					}

					public void onMouseUp(Widget sender, int x, int y) {
						eventInfo.setText("onMouseUp: "+x+", "+y);	
					}
	    			
	    		});
	    		
	    		
	    		
	    		log("Output panel set");

	    		CanvasDrawingStyle radgrad = ctx.createRadialGradient(45,45,10,52,50,30);
	    		radgrad.addColorStop(0, "#A7D30C");
	    		radgrad.addColorStop((float) 0.9, "#019F62");
	    		radgrad.addColorStop(1, "rgba(1,159,98,0)");
	    		  
	    		CanvasDrawingStyle radgrad2 = ctx.createRadialGradient(105,105,20,112,120,50);
	    		radgrad2.addColorStop(0, "#FF5F98");
	    		radgrad2.addColorStop((float) 0.75, "#FF0188");
	    		radgrad2.addColorStop(1, "rgba(255,1,136,0)");

	    		CanvasDrawingStyle radgrad3 = ctx.createRadialGradient(95,15,15,102,20,40);
	    		radgrad3.addColorStop(0, "#00C9FF");
	    		radgrad3.addColorStop((float) 0.8, "#00B5E2");
	    		radgrad3.addColorStop(1, "rgba(0,201,255,0)");

	    		CanvasDrawingStyle radgrad4 = ctx.createRadialGradient(0,150,50,0,140,90);
	    		radgrad4.addColorStop(0, "#F4F201");
	    		radgrad4.addColorStop((float) 0.8, "#E4C700");
	    		radgrad4.addColorStop(1, "rgba(228,199,0,0)");
	    		log("Created radial gradients");  
	    		
	    		ctx.setFillStyle(radgrad4);
	    		ctx.fillRect(0,0,150,150);
	    		ctx.setFillStyle(radgrad3);
	    		ctx.fillRect(0,0,150,150);
	    		ctx.setFillStyle(radgrad2);
	    		ctx.fillRect(0,0,150,150);
	    		ctx.setFillStyle(radgrad);
	    		ctx.fillRect(0,0,150,150);
	    		log("draw shapes");
	    } 
	}
}
