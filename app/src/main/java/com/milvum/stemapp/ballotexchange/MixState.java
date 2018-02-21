package com.milvum.stemapp.ballotexchange;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.milvum.stemapp.R;

/**
 * .
 */

public enum MixState {
    IDLE,
    REQUEST,
    PAY,
    WHISPER,
    FINISH;

    private static final String MIX_STATE = "MixState";
    private static MixState state;

    public static MixState getMixState(Context context) {
        if(state == null) {
            SharedPreferences preferences = context.getSharedPreferences(
                    context.getString(R.string.preferenceFile), Context.MODE_PRIVATE);

            String stateName = preferences.getString(MIX_STATE, IDLE.name());
            state = valueOf(stateName);
        }

        return state;
    }

    public static void setMixState(Context context, MixState state) {
        Log.d("Mixing", "Switching from state " + getMixState(context).name() + " to " + state.name());

        MixState.state = state;

        SharedPreferences.Editor prefEditor = context.getSharedPreferences(
                context.getString(R.string.preferenceFile), Context.MODE_PRIVATE).edit();

        prefEditor.putString(MIX_STATE, state.name());
        prefEditor.apply();

    }
}
