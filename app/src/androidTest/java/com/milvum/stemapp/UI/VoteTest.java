package com.milvum.stemapp.UI;


import android.Manifest;
import android.support.test.espresso.DataInteraction;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class VoteTest
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
     * This test opens the Candidate List screen and asserts all the components.
     * This test also checks every box.
     * Starts with opening the Home Activity, then navigates to the Candidate List Activity.
     * Before navigating to the Candidate List the test checks whether the user has received the voting PASS.
     *
     * @throws InterruptedException
     */
    @Test
    public void voteTest() throws InterruptedException
    {
        while (!TestHelpers.isAbleToVote(mActivityTestRule.getActivity().getApplicationContext()))
        {
            Thread.sleep(1000);
        }

        // HOME ACTIVITY
        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.nextButton), withText("Stemmen"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                0),
                        isDisplayed()));
        appCompatButton2.perform(click());

        Thread.sleep(3000);

        // CANDIDATE LIST ACTIVITY
        ViewInteraction relativeLayout = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.candidate_list),
                                childAtPosition(
                                        withId(R.id.activity_party_list),
                                        0)),
                        0),
                        isDisplayed()));
        relativeLayout.check(matches(isDisplayed()));

        ViewInteraction button = onView(
                allOf(withId(R.id.voteButton),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction relativeLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.candidate_list),
                                childAtPosition(
                                        withId(R.id.activity_party_list),
                                        0)),
                        0),
                        isDisplayed()));
        relativeLayout2.check(matches(isDisplayed()));

        DataInteraction relativeLayout3 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(0);
        relativeLayout3.perform(click());

        DataInteraction relativeLayout4 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(1);
        relativeLayout4.perform(click());

        DataInteraction relativeLayout5 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(2);
        relativeLayout5.perform(click());

        DataInteraction relativeLayout6 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(3);
        relativeLayout6.perform(click());

        DataInteraction relativeLayout7 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(4);
        relativeLayout7.perform(click());

        DataInteraction relativeLayout8 = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(5);
        relativeLayout8.perform(click());

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
