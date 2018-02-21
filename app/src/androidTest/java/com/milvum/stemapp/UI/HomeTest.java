package com.milvum.stemapp.UI;


import android.Manifest;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.milvum.stemapp.HomeActivity;
import com.milvum.stemapp.R;
import com.milvum.stemapp.TestHelpers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class HomeTest
{

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);

    @Rule
    public ActivityTestRule<HomeActivity> mActivityTestRule = new ActivityTestRule<>(HomeActivity.class, false, false);


    /**
     * Clears all app data and cache.
     * This ensures a clean testing environment.
     */
    @Before
    public void setup()
    {
        TestHelpers.clearState();
        mActivityTestRule.launchActivity(null);
    }


    /**
     * This test opens the Home screen and asserts all the components.
     * This test also checks whether the app opens/closes the QR code popup.
     * Starts with opening the Home Activity.
     *
     * @throws InterruptedException
     */
    @Test
    public void homeTest() throws InterruptedException
    {
        // HOME ACTIVITY
        ViewInteraction imageButton = onView(
                allOf(withId(R.id.qrImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        imageButton.check(matches(isDisplayed()));

        ViewInteraction appCompatButton = onView(
                Matchers.allOf(ViewMatchers.withId(R.id.qrImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        appCompatButton.perform(click());

        Thread.sleep(5000);

        ViewInteraction appCompatButton6 = onView(
                Matchers.allOf(ViewMatchers.withId(R.id.button), withText("OK"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()));
        appCompatButton6.perform(click());

        Thread.sleep(2000);

        ViewInteraction linearLayout = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(R.id.activity_home),
                                0),
                        1),
                        isDisplayed()));
        linearLayout.check(matches(isDisplayed()));

        ViewInteraction textView = onView(
                allOf(withText("StemApp"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        textView.check(matches(withText("StemApp")));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.logo),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        1),
                                0),
                        isDisplayed()));
        imageView3.check(matches(isDisplayed()));

        ViewInteraction button2 = onView(
                allOf(withId(R.id.nextButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                0),
                        isDisplayed()));
        button2.check(matches(isDisplayed()));

        ViewInteraction linearLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.activity_home),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        0),
                        isDisplayed()));
        linearLayout2.check(matches(isDisplayed()));

        ViewInteraction linearLayout3 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.activity_home),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        0),
                        isDisplayed()));
        linearLayout3.check(matches(isDisplayed()));

    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position)
    {

        return new TypeSafeMatcher<View>()
        {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
