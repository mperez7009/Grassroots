package com.codepath.gogreen.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.gogreen.ActionAdapter;
import com.codepath.gogreen.R;
import com.codepath.gogreen.models.Action;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anyazhang on 7/13/17.
 */

public class FeedFragment extends Fragment {
    ArrayList<Action> actions;
    RecyclerView rvActions;
    ActionAdapter actionAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ParseQuery<Action> query = ParseQuery.getQuery("Action");
        query.findInBackground(new FindCallback<Action>() {
            public void done(List<Action> actionList, ParseException e) {
                if (e == null) {
                    addItems(actionList);
                } else {
                    Log.d("action", "Error: " + e.getMessage());
                }
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_feed, container, false);
        rvActions = (RecyclerView) v.findViewById(R.id.rvFeed);
        actions = new ArrayList<>();
        actionAdapter = new ActionAdapter(actions);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvActions.setLayoutManager(linearLayoutManager);
        // set the adapter
        rvActions.setAdapter(actionAdapter);

        return v;
    }

    public void addItems(List<Action> actionList) {
        // iterate through JSON array
        // for each entry, deserialize the JSON object
        Log.d("addItems", String.valueOf(actionList.size()));
        for (int i = 0; i < actionList.size(); i++) {
            Action action = actionList.get(i);
            actions.add(action);
            actionAdapter.notifyItemInserted(actions.size() - 1);
            Log.d("addItems", String.valueOf(actions.size()) + ": " + action.get("actionType"));

        }
    }
}