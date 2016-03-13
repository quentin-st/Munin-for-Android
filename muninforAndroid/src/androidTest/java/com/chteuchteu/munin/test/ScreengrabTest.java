package com.chteuchteu.munin.test;

import android.support.test.rule.ActivityTestRule;
import android.test.ActivityUnitTestCase;

import com.chteuchteu.munin.ui.Activity_Main;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@RunWith(JUnit4.class)
public class ScreengrabTest extends ActivityUnitTestCase<Activity_Main> {
	public ScreengrabTest() {
		super(Activity_Main.class);
	}

	@ClassRule
	public static final LocaleTestRule localeTestRule = new LocaleTestRule();

	@Rule
	public ActivityTestRule<Activity_Main> activityRule = new ActivityTestRule<>(Activity_Main.class);

	@Test
	public void testTakeScreenshot()
	{
		Screengrab.screenshot("Home");

		// Navigate to another screen and take another screenshot
		Screengrab.screenshot("Home2");
	}
}
