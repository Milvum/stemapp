package com.milvum.stemapp.UI;


import android.Manifest;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Toast;

import com.milvum.stemapp.HomeActivity;
import com.milvum.stemapp.R;
import com.milvum.stemapp.TestHelpers;
import com.milvum.stemapp.ballotexchange.ExchangeScheduler;
import com.milvum.stemapp.ballotexchange.MixJob;
import com.milvum.stemapp.geth.implementation.Wallet;
import com.milvum.stemapp.geth.implementation.Web3jProvider;
import com.milvum.stemapp.geth.implementation.contract.VotingBallotUtil;
import com.milvum.stemapp.geth.implementation.contract.VotingPassUtil;
import com.milvum.stemapp.model.WalletRole;
import com.milvum.stemapp.utils.Constants;
import com.milvum.stemapp.utils.ToastUtil;
import com.milvum.stemapp.utils.VoteUtil;
import com.milvum.stemapp.utils.WalletUtil;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.io.File;
import java.math.BigInteger;

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
public class VoteVerificationTest
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
     * This test casts a vote on the first candidate displayed at the Candidate List Activity, then verifies the vote casted.
     * Starts with opening the Home Activity, then navigates to the Candidate List Activity.
     * Before navigating to the Candidate List the test checks whether the user has received the voting PASS.
     * Before casting a vote, the test waits until a voting BALLOT is received.
     * After casting a vote, the user is returned to the Home Activity.
     * After returning to the Home Activity the test navigates to the Verification Activity (after checking the correct amount of total votes needed).
     * The test presses a letter displayed and navigates back to the Home Activity.
     * Here the QR Popup is opened and closed.
     *
     * @throws InterruptedException
     */
    @Test
    public void voteVerificationTest() throws Exception
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

        // POPUP (ACCEPTED)
        ViewInteraction appCompatButton4 = onView(
                allOf(withId(R.id.yesButton), withText("Ja"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2),
                                0),
                        isDisplayed()));
        appCompatButton4.perform(click());

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

        // HOME ACTIVITY
        ViewInteraction appCompatButton5 = onView(
                allOf(withId(R.id.homeButton), withText("Naar thuisscherm"),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        appCompatButton5.perform(click());

        while (TestHelpers.isAbleToVerifyVote(mActivityTestRule.getActivity().getApplicationContext()))
        {
            Thread.sleep(1000);
        }

        // VERIFICATION ACTIVITY
        ViewInteraction appCompatButton7 = onView(
                allOf(withId(R.id.verifyButton), withText("Controleer uw uitgebrachte stem"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                1),
                        isDisplayed()));
        appCompatButton7.perform(click());

        Thread.sleep(3000);

        ViewInteraction gridView = onView(
                allOf(withId(R.id.vote_grid),
                        childAtPosition(
                                allOf(withId(R.id.activity_voting_tiles),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        gridView.check(matches(isDisplayed()));

        DataInteraction relativeLayout2 = onData(anything())
                .inAdapterView(allOf(withId(R.id.vote_grid),
                        childAtPosition(
                                withId(R.id.activity_voting_tiles),
                                0)))
                .atPosition(0);
        relativeLayout2.perform(click());

        Thread.sleep(Constants.SECRET_WAIT_TIME * 2);

        ViewInteraction appCompatButton10 = onView(
                allOf(withId(R.id.homeButton), withText("Naar thuisscherm"),
                        childAtPosition(
                                allOf(withId(R.id.activity_party_list),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                0),
                        isDisplayed()));
        appCompatButton10.perform(click());

        Thread.sleep(3000);

        // HOME ACTIVITY
        ViewInteraction linearLayout5 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.activity_home),
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0)),
                        0),
                        isDisplayed()));
        linearLayout5.check(matches(isDisplayed()));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.nextButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                0),
                        isDisplayed()));
        button3.check(matches(isDisplayed()));

        ViewInteraction button4 = onView(
                allOf(withId(R.id.verifyButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.activity_home),
                                        1),
                                1),
                        isDisplayed()));
        button4.check(matches(isDisplayed()));

        ViewInteraction imageButton4 = onView(
                allOf(withId(R.id.qrImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        imageButton4.check(matches(isDisplayed()));

        // QR POPUP
        ViewInteraction appCompatButton3 = onView(
                Matchers.allOf(ViewMatchers.withId(R.id.qrImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                0),
                        isDisplayed()));
        appCompatButton3.perform(click());

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

        Thread.sleep(3000);

        // HOME ACTIVITY
        ViewInteraction imageButton5 = onView(
                allOf(withId(R.id.quitImageButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        0),
                                1),
                        isDisplayed()));
        imageButton5.check(matches(isDisplayed()));

        ViewInteraction imageView3 = onView(
                allOf(withId(R.id.logo),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        1),
                                0),
                        isDisplayed()));
        imageView3.check(matches(isDisplayed()));

        ViewInteraction textView7 = onView(
                allOf(withText("StemApp"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        textView7.check(matches(withText("StemApp")));

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
