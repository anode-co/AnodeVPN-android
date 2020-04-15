package com.anode.anode;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class FirstFragment extends Fragment {
    private static String LOGTAG = "FirstFragment";
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_getPeers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                try {
                    AdminApi api = AdminApi.from("/data/data/com.anode.anode/files/cjdroute.conf");
                    //api.getPeers()
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to get peers using Admin API", e);
                    e.printStackTrace();
                }

            }
        });

        view.findViewById(R.id.button_ping).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                try {
                    AdminApi api = AdminApi.from("/data/data/com.anode.anode/files/cjdroute.conf");
                    api.ping();
                    //api.getPeers()
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to ping using Admin API", e);
                    e.printStackTrace();
                }

            }
        });


    }
}
