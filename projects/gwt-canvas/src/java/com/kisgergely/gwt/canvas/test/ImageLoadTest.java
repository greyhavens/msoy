package com.kisgergely.gwt.canvas.test;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.Widget;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;
import com.kisgergely.gwt.tester.client.TestLoadListener;

public class ImageLoadTest extends ClientTestCase {

	public ImageLoadTest() {
		super("Image Load Test");
	}

	
	public void doTest(final ClientTestCaseContext testCtx) {
    		final Image i = new Image("test/pattern.png");
    		log("Image created");
    		i.addLoadListener(new TestLoadListener(testCtx, new LoadListener() {
					public void onError(Widget sender) {
						assertFail("Image test/pattern.png could not be loaded");
					}

					public void onLoad(Widget sender) {
						log("Image loaded");
				        testCtx.finishedTest(ClientTestCaseContext.SUCCESS);
					}
	    		}));
    		i.setVisible(true);
    		testCtx.getOutputPanel().add(i);
	    	testCtx.waitForTest();
	}
}
