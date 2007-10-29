package com.kisgergely.gwt.canvas.test;

import com.google.gwt.user.client.ui.Label;
import com.kisgergely.gwt.tester.client.ClientTestCase;
import com.kisgergely.gwt.tester.client.ClientTestCaseContext;

public class DummyTest extends ClientTestCase {

	public DummyTest() {
		super("Dummy Test");
	}

	public void doTest(ClientTestCaseContext testCtx) {
		Label l = new Label("Dummy Test");
		log("Label created");
		testCtx.getOutputPanel().add(l);
		log("Label added to Output panel");
	}
}
