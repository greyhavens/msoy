package com.kisgergely.gwt.canvas.test;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasDrawingStyle;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;

public class CanvasLinearGradientTest extends ClientTestCase {

	public CanvasLinearGradientTest() {
		super("Canvas Linear Gradient Test");
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
	    		log("Setting output widget");
	    		assertNotNull(testCtx.getOutputPanel(),"GetOutputPanel");
	    		testCtx.getOutputPanel().add(c);
	    		
	    		// This is necessary because otherwise the Canvas
	    		// is not always displayed in Firefox 1.5
	    		testCtx.getOutputPanel().setSize(""+c.getOffsetWidth(),""+c.getOffsetHeight());
	    		
	    		log("Output panel set");

	    		  CanvasDrawingStyle lingrad = ctx.createLinearGradient(0,0,0,150);
	    		  lingrad.addColorStop(0, "#00ABEB");
	    		  lingrad.addColorStop((float) 0.5, "#fff");
	    		  lingrad.addColorStop((float) 0.5, "#26C000");
	    		  lingrad.addColorStop(1, "#fff");

	    		  CanvasDrawingStyle lingrad2 = ctx.createLinearGradient(0,50,0,95);
	    		  lingrad2.addColorStop((float) 0.5, "#000");
	    		  lingrad2.addColorStop(1, "rgba(0,0,0,0)");
	    		  log("Create gradients");

	    		  ctx.setFillStyle(lingrad);
	    		  ctx.setStrokeStyle(lingrad2);
	    		  log("assign gradients to fill and stroke styles");
	    		  
	    		  
	    		  ctx.fillRect(10,10,130,130);
	    		  ctx.strokeRect(50,50,50,50);
	    		  log("Draw shapes");
	    		
	    } 
	}
}
