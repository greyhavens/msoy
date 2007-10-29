package com.kisgergely.gwt.canvas.test;

import com.kisgergely.gwt.canvas.client.Canvas;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;

public class CanvasCreationTest extends ClientTestCase {

	public CanvasCreationTest() {
		super("Canvas Creation Test");
	}

	public void doTest(ClientTestCaseContext ctx) {
		Canvas c = new Canvas(300,300);
		
		log("Canvas created");
		// The canvas should be there
		assertNotNull(c,"Canvas");
		
		// The 2D context should be available
		assertNotNull(c.getContext2D(),"Ctx2D");
		log("GetContext done");
		
		// Only one instance of 2D context should exist
		assertTrue(c.getContext2D() == c.getContext2D(),"Ctx2d with itself");
		log("GetContext there done");
		
		// A data URL of mime-type image/png should be returned
		String pngDataURL = c.toDataURL();
		log("1");
		assertTrue(pngDataURL == null || 
				pngDataURL.startsWith("data:image/png"),"pngDataURL startsWith");
		
		// For unsupported image types an image/png type 
		// data URL should be returned
		String unsuppDataURL = c.toDataURL("image/not-existing-image-type");
		log("2");
		assertTrue(unsuppDataURL == null || unsuppDataURL.startsWith("data:image/png"),"pngDataURL startsWith unsupported");		
	}

}
