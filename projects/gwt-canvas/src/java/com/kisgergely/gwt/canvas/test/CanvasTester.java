package com.kisgergely.gwt.canvas.test;

import com.kisgergely.gwt.tester.client.ClientTester;


public class CanvasTester extends ClientTester {
	
	public CanvasTester() {
		super("Canvas Tester Tool");
	}
	
	protected void loadTestCases() {
		addTestCase(new DummyTest());
		addTestCase(new ImageLoadTest());
		addTestCase(new CanvasCreationTest());
		addTestCase(new CanvasDrawTest());
		addTestCase(new CanvasPatternTest());
		addTestCase(new CanvasLinearGradientTest());
		addTestCase(new CanvasRadialGradientTest());
		addTestCase(new CanvasMouseTest());
	}	
}
