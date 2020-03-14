package com.example.rick_and_morty_quiz;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import safety.com.br.android_shake_detector.core.ShakeCallback;
import safety.com.br.android_shake_detector.core.ShakeDetector;
import safety.com.br.android_shake_detector.core.ShakeOptions;

public class DynamicFragmentSearchBar extends Fragment {

    private SearchConnectionTask searchTask;
    Activity activity;
    View view;
    private ShakeDetector shakeDetector;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            activity =(Activity) context; // here, we get the instance of the current activity to use its functions
        }
    }

    public DynamicFragmentSearchBar() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.searchbar_dynamic_fragment, container, false);
        view = rootView;
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) { // we do what we want once the view is created, not before
        final EditText searchBar = (EditText) view.findViewById(R.id.searchBar);
        ShakeOptions options = new ShakeOptions()
                .background(true)
                .interval(1000)
                .shakeCount(2)
                .sensibility(2.0f);

        this.shakeDetector = new ShakeDetector(options).start(activity, new ShakeCallback() { //we do the search if phone is shaken
            @Override
            public void onShake() {
                String searchedText = searchBar.getText().toString();
                if (!searchedText.equals("")) {//we do something upon shaking only if the user entered something to search
                    if (searchTask != null)
                        searchTask.cancel(true);
                    searchTask = new SearchConnectionTask(searchedText);
                    Toast toast = Toast.makeText(activity.getApplicationContext(), "     SEARCHING,\n   PLEASE WAIT...", Toast.LENGTH_LONG);
                    LinearLayout toastLayout = (LinearLayout) toast.getView();
                    TextView toastTV = (TextView) toastLayout.getChildAt(0);
                    toastTV.setTextSize(30);
                    toast.setGravity(Gravity.TOP, 0, 150);
                    toast.show();
                    searchTask.execute();
                }
            }
        });
        searchBar.post(new Runnable() {
            @Override
            public void run() { //same, but when enter key is pressed
                searchBar.setOnKeyListener(new View.OnKeyListener() {
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        // Event is a key-down event on the "enter" button
                        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                                (keyCode == KeyEvent.KEYCODE_ENTER)) {
                            String searchedText = searchBar.getText().toString();
                            if (searchTask != null)
                                searchTask.cancel(true);
                            searchTask = new SearchConnectionTask(searchedText);
                            Toast toast = Toast.makeText(activity.getApplicationContext(),"     SEARCHING,\n   PLEASE WAIT...", Toast.LENGTH_LONG);
                            LinearLayout toastLayout = (LinearLayout) toast.getView();
                            TextView toastTV = (TextView) toastLayout.getChildAt(0);
                            toastTV.setTextSize(30);
                            toast.setGravity(Gravity.TOP, 0, 150);
                            toast.show();
                            searchTask.execute();
                            return true;
                        }
                        return false;
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        shakeDetector.destroy(activity.getBaseContext());
        super.onDestroy();
    }

    private class SearchConnectionTask extends AsyncTask<Void, Void, JSONObject> {
        private String str;
        private ArrayList<String> character;

        public SearchConnectionTask(String str) {
            this.str = str;
            character = new ArrayList<>();
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            for (int i = 1; i < 26; ++i) { //sadly, because of the way the API is (can only directly find something based on id and not name), we search the whole of it every time
                Log.i("meh", "i: " + i);
                String urlStr = "https://rickandmortyapi.com/api/character/";
                if (i != 1)
                    urlStr += "?page=" + i;
                try {
                    URL url = new URL(urlStr);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    String res = readStream(in);
                    JSONObject json = new JSONObject(res);
                    final JSONArray characters = json.getJSONArray("results");
                    for (int j = 0; j < characters.length(); ++j) {
                        Log.i("meh", "j: " + j);
                        JSONObject tmp = characters.getJSONObject(j);
                        if (tmp.getString("name").toUpperCase().contains(str.toUpperCase())) { // this line does the selection based on what the user entered
                            character.add(tmp.toString());
                        }
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (character.size() == 0) { // no result was found
                        Toast toast = Toast.makeText(activity.getApplicationContext(), "No result for " + str, Toast.LENGTH_SHORT);
                        LinearLayout toastLayout = (LinearLayout) toast.getView();
                        TextView toastTV = (TextView) toastLayout.getChildAt(0);
                        toastTV.setTextSize(30);
                        toast.setGravity(Gravity.TOP, 0, 150);
                        toast.show();
                    }
                    else { // result(s) was/were found
                        Intent intent = new Intent(activity, EntryActivity.class);
                        Log.i("meh", character.get(0).toString());
                        Gson gson = new Gson();
                        String json = gson.toJson(character);
                        intent.putExtra("character", json);
                        activity.finish();
                        startActivity(intent);
                    }
                }
            });
            return null;
        }
    }

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while(i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (IOException e) {
            return "";
        }
    }

}
