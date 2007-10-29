package com.kisgergely.gwt.canvas.test;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.Widget;
import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasDrawingStyle;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;
import com.kisgergely.gwt.tester.client.TestLoadListener;

public class CanvasPatternTest extends ClientTestCase {

	public CanvasPatternTest() {
		super("Canvas Pattern Test");
	}

	
	public void doTest(final ClientTestCaseContext testCtx) {
		Canvas c = new Canvas(350,350);
		log("Canvas created");
		// The canvas should be there
		assertNotNull(c, "Canvas");
				
	    final CanvasRenderingContext2D ctx = c.getContext2D();
	    log("Context acquired");
	    
	    assertNotNull(ctx, "Ctx2d");

	    if (ctx != null)
	    {	
	    		log("Setting output widget");
	    		assertNotNull(testCtx.getOutputPanel(),"GetOutputPanel");
	    		testCtx.getOutputPanel().add(c);
	    		
	    		// This is necessary because otherwise the Canvas
	    		// is not always displayed in Firefox 1.5
	    		testCtx.getOutputPanel().setSize(""+c.getOffsetWidth(),""+c.getOffsetHeight());
	    		
	    		log("Output panel set");
	    		final Image i = new Image("test/pattern.png");
	    		i.addLoadListener(new TestLoadListener(testCtx, new LoadListener() {
					public void onError(Widget sender) {
						assertFail("Image test/pattern.png could not be loaded");
					}

					public void onLoad(Widget sender) {
				        CanvasDrawingStyle fStyle = ctx.createPattern(i,"");
				        log("2");
				    		ctx.setFillStyle(fStyle);
				        log("3");
				        ctx.fillRect(20,20,300,300);
				        log("4");
				        ctx.setFillStyle("rgb(200,0,0)");
				        log("5");
				        ctx.fillRect(10,10,50,50);
				        log("6");
				        testCtx.finishedTest(ClientTestCaseContext.SUCCESS);
					}
	    		}));
	    		i.setVisible(true);
	    		testCtx.getResourcePanel().add(i);
	    		log("1");
	    		testCtx.waitForTest();
	    } 
	}
}
