package com.kisgergely.gwt.canvas.test;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.canvas.client.CanvasRenderingContext2D;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;

public class CanvasDrawTest extends ClientTestCase {

	public CanvasDrawTest() {
		super("Canvas Draw Test");
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
	        ctx.setFillStyle("rgb(200,0,0)");
	        log("1");
	        ctx.fillRect(10,10,50,50);
	        log("2");
	        ctx.setFillStyle("rgba(0,0,200,0.5)");
	        log("3");
	        ctx.fillRect(30,30,50,50);
	        log("4");
	        int offset = 200;
	        ctx.beginPath();
	        ctx.moveTo(offset+75,offset+50);
	        ctx.lineTo(offset+100,offset+75);
	        ctx.lineTo(offset+100,offset+25);
	        ctx.fill();
	        
	        ctx.beginPath();
	        offset = 100;
	        ctx.arc(offset+75,offset+75,50,0,(float)Math.PI*2,true); // Outer circle
	        ctx.moveTo(offset+110,offset+75);
	        ctx.arc(offset+75,offset+75,35,0,(float)Math.PI,false);   // Mouth (clockwise)
	        ctx.moveTo(offset+65,offset+65);
	        ctx.arc(offset+60,offset+65,5,0,(float)Math.PI*2,true);  // Left eye
	        ctx.moveTo(offset+95,offset+65);
	        ctx.arc(offset+90,offset+65,5,0,(float)Math.PI*2,true);  // Right eye
	        ctx.stroke();    
	    } 
	}

}
