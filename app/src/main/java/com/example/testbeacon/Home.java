package com.example.testbeacon;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

// Home Fragment (PreferenceFragment) which is embedded in MainActivity shows detected beacon
// on fragment swiperefreshlayout is places
public class Home extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public Home() {
        // Required empty public constructor
    }


    public static Home newInstance(String param1, String param2) {
        Home fragment = new Home();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    // this method is called after the fragment is created (user navigates to fragment)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // event listener of swipe refresh layout (home fragment) starts beacon scanning
        final SwipeRefreshLayout pullToRefresh = ((MainActivity)getActivity()).findViewById(R.id.swiperefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((MainActivity)getActivity()).scanClicked(((MainActivity)getActivity()).findViewById(R.id.fragment_home));
                // when beacon is detected refreshing animation will be stopped
                //pullToRefresh.setRefreshing(false);
            }
        });
    }



}