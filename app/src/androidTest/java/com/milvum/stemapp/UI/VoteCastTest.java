package com.milvum.stemapp.UI;


import android.Manifest;
import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
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
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class VoteCastTest
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
     * This test casts a vote on the first candidate displayed at the Candidate List Activity.
     * Starts with opening the Home Activity, then navigates to the Candidate List Activity.
     * Before navigating to the Candidate List the test checks whether the user has received the voting PASS.
     * Before casting a vote, the test waits until a voting BALLOT is received and also tests the popup.
     * After casting a vote, the user is returned to the Home Activity.
     *
     * @throws InterruptedException
     */
    @Test
    public void voteCastTest() throws Exception
    {

        // HOME ACTIVITY
        while (!TestHelpers.isAbleToVote(mActivityTestRule.getActivity().getApplicationContext()))
        {
            Thread.sleep(1000);
        }

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
        DataInteraction relativeLayout = onData(anything())
                .inAdapterView(allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                withId(R.id.activity_party_list),
                                0)))
                .atPosition(0);
        relativeLayout.perform(click());

        while (!TestHelpers.hasReceivedBallot(mActivityTestRule.getActivity().getApplicationContext()))
        {
            Thread.sleep(1000);
        }

        do
        {
            try
            {
                ViewInteraction appCompatButton3 = onView(
                        allOf(withId(R.id.voteButton), withText("Stem"),
                                childAtPosition(
                                        allOf(withId(R.id.activity_party_list),
                                                childAtPosition(
                                                        withId(android.R.id.content),
                                                        0)),
                                        1),
                                isDisplayed()));
                appCompatButton3.perform(click());
                break;
            } catch (Throwable t)
            {
                Thread.sleep(500);
            }
        }
        while (true);

        // POPUP OPENED
        ViewInteraction linearLayout = onView(
                allOf(childAtPosition(
                        allOf(withId(android.R.id.content),
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.FrameLayout.class),
                                        0)),
                        0),
                        isDisplayed()));
        linearLayout.check(matches(isDisplayed()));

        ViewInteraction textView = onView(
                allOf(withId(R.id.title), withText("U stemt op"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("U stemt op")));

        ViewInteraction linearLayout2 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(android.R.id.content),
                                0),
                        0),
                        isDisplayed()));
        linearLayout2.check(matches(isDisplayed()));

        ViewInteraction button = onView(
                allOf(withId(R.id.yesButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        2),
                                0),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction button2 = onView(
                allOf(withId(R.id.noButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        2),
                                1),
                        isDisplayed()));
        button2.check(matches(isDisplayed()));

        // POPUP CLOSED (PRESSED NO)
        ViewInteraction appCompatButton4 = onView(
                allOf(withId(R.id.noButton), withText("Nee"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                1),
                        isDisplayed()));
        appCompatButton4.perform(click());

        Thread.sleep(3000);

        ViewInteraction listView = onView(
                allOf(withId(R.id.candidate_list),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        listView.check(matches(isDisplayed()));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.voteButton),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()));
        button3.check(matches(isDisplayed()));

        ViewInteraction relativeLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.candidate_list),
                                childAtPosition(
                                        withId(R.id.activity_party_list),
                                        0)),
                        0),
                        isDisplayed()));
        relativeLayout2.check(matches(isDisplayed()));

        ViewInteraction appCompatButton5 = onView(
                allOf(withId(R.id.voteButton), withText("Stem"),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatButton5.perform(click());

        Thread.sleep(3000);

        // POPUP OPENED
        ViewInteraction linearLayout3 = onView(
                allOf(childAtPosition(
                        childAtPosition(
                                withId(android.R.id.content),
                                0),
                        0),
                        isDisplayed()));
        linearLayout3.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.title), withText("U stemt op"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("U stemt op")));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.title), withText("U stemt op"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        textView3.check(matches(isDisplayed()));

        ViewInteraction linearLayout4 = onView(
                allOf(withId(R.id.vote_info),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                1),
                        isDisplayed()));
        linearLayout4.check(matches(isDisplayed()));

        ViewInteraction button4 = onView(
                allOf(withId(R.id.yesButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        2),
                                0),
                        isDisplayed()));
        button4.check(matches(isDisplayed()));

        ViewInteraction button5 = onView(
                allOf(withId(R.id.noButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        2),
                                1),
                        isDisplayed()));
        button5.check(matches(isDisplayed()));

        // POPUP ACCEPTED (PRESSED YES)
        ViewInteraction appCompatButton6 = onView(
                allOf(withId(R.id.yesButton), withText("Ja"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        appCompatButton6.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton7 = onView(
                allOf(withId(R.id.homeButton), withText("Naar thuisscherm"),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        appCompatButton7.perform(click());

        Thread.sleep(3000);

        // HOME ACTIVITY
        ViewInteraction imageButton2 = onView(
                allOf(withId(R.id.quitImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                1),
                        isDisplayed()));
        imageButton2.check(matches(isDisplayed()));

        ViewInteraction button9 = onView(
                allOf(withId(R.id.verifyButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                1),
                        isDisplayed()));
        button9.check(matches(isDisplayed()));

        ViewInteraction imageButton3 = onView(
                allOf(withId(R.id.qrImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        imageButton3.check(matches(isDisplayed()));

        ViewInteraction button10 = onView(
                allOf(withId(R.id.nextButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                0),
                        isDisplayed()));
        button10.check(matches(isDisplayed()));

        ViewInteraction linearLayout6 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.activity_home),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        0),
                        isDisplayed()));
        linearLayout6.check(matches(isDisplayed()));

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
