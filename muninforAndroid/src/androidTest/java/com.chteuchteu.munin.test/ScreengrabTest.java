package com.chteuchteu.munin.test;

import android.support.test.rule.ActivityTestRule;
import android.test.suitebuilder.annotation.LargeTest;

import com.chteuchteu.munin.R;
import com.chteuchteu.munin.hlpr.TestsHelper;
import com.chteuchteu.munin.ui.Activity_Main;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withTagValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.isq;

@RunWith(JUnit4.class)
@LargeTest
public class ScreengrabTest {
	@ClassRule
	public static final LocaleTestRule localeTestRule = new LocaleTestRule();

	@Rule
	public ActivityTestRule<Activity_Main> activityRule = new ActivityTestRule<>(Activity_Main.class);

	@Test
	public void testTakeScreenshot() {
		// Main screen screenshot
		Screengrab.screenshot("Activity_Main");

		// Servers screenshot
		// Tap on "Servers" in drawer
		clickOnDrawerItem(TestsHelper.DRAWER_SERVERS);
		// Tap on fab
		onView(withId(R.id.fab)).perform(click());

		// Type test server URL & hit enter
		onView(withId(R.id.textbox_serverUrl))
				.perform(typeText("http://munin.ping.uio.no"));

		Screengrab.screenshot("Activity_Server");

		/*onView(withId(R.id.textbox_serverUrl)).perform(pressKey(KeyEvent.KEYCODE_ENTER));

		// Taking screenshots of AlertDialogs makes screengrab crash for now
		//Screengrab.screenshot("Activity_Server-Dialog");

		// Tap on "OK" on the AlertDialog
		onView(withId(R.id.popup_button)).perform(click());*/

		// Plugins list
		openDrawer();
		clickOnDrawerItem(TestsHelper.DRAWER_GRAPHS);
		Screengrab.screenshot("Activity_Plugins");

		// Grids
		openDrawer();
		clickOnDrawerItem(TestsHelper.DRAWER_GRIDS);
		// First grid
		clickInListView(R.id.listview, 0);
		Screengrab.screenshot("Activity_Grids");

		// Alerts
		openDrawer();
		clickOnDrawerItem(TestsHelper.DRAWER_ALERTS);
		Screengrab.screenshot("Activity_Alerts");

		// Graphs
		openDrawer();
		clickOnDrawerItem(TestsHelper.DRAWER_GRAPHS);
		clickInListViewWithText(R.id.listview, "Disk usage in percent");
		Screengrab.screenshot("Activity_GraphView");
		// Documentation
		onView(withId(R.id.menu_documentation)).perform(click());
		Screengrab.screenshot("Activity_GraphView-Documentation");
	}

	private void openDrawer() {
		//onView(withId(R.id.material_drawer_layout)).perform(DrawerActions.open());
		onView(allOf(withContentDescription("Open"), isClickable()))
				.perform(click());
	}

	private void clickOnDrawerItem(String tag) {
		onView(withTagValue(is((Object) tag))).perform(click());
	}

	private void clickInListViewWithText(int listId, String text) {
		onData(hasToString(containsString(text)))
				.inAdapterView(withId(listId)).atPosition(0)
				.perform(click());
	}

	private void clickInListView(int listId, int position) {
		onData(anything())
				.inAdapterView(withId(listId)).atPosition(position
		).perform(click());
	}
}
