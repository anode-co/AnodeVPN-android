package com.anode.anode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;

public abstract class Cjdroute {

    static final String FILENAME_CJDROUTE = "cjdroute";
    private static final String SHARED_PREFERENCES_KEY_CJDROUTE_PID = "cjdroutePid";
    private static final int INVALID_PID = Integer.MIN_VALUE;
/*
    abstract Subscriber<JSONObject> execute();

    abstract Subscriber<Integer> terminate();

    @Override
    public Subscriber<JSONObject> execute() {
        return new Subscriber<JSONObject>() {
            @Override
            public void onNext(JSONObject cjdrouteConf) {
                DataOutputStream os = null;
                try {
                    java.lang.Process process = Runtime.getRuntime().exec("su");

                    final InputStream iStream = process.getInputStream();
                    InputStreamO
                }
            }
        }
    }

 */
}
