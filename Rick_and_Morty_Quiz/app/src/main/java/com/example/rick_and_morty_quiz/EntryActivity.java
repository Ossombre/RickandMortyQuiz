package com.example.rick_and_morty_quiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

public class EntryActivity extends AppCompatActivity {

    HttpConnectionTask httpConnectionTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        if (savedInstanceState == null) { //searchbar fragment call
            getFragmentManager().beginTransaction()
                    .add(R.id.containerEntry, new DynamicFragmentSearchBar()).commit();
        }
        if (httpConnectionTask != null)
            httpConnectionTask.cancel(true);
        httpConnectionTask = new HttpConnectionTask();
        httpConnectionTask.execute(); //main asyncTask call
        Button buttonRandomChar = (Button) findViewById(R.id.buttonRandom); //button to get a new random character (by reopening the main activity)
        buttonRandomChar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(EntryActivity.this, MainActivity.class));
            }
        });
    }

    private class HttpConnectionTask extends AsyncTask<Void, Void, String> {

        MyRecyclerViewAdapter adapter;

        @Override
        protected String doInBackground(Void... voids) {
            String json = getIntent().getStringExtra("character");
            Gson gson = new Gson();
            final ArrayList<String> characters = gson.fromJson(json, ArrayList.class);
            try {
                JSONObject character = new JSONObject(characters.get(0));
                String imageUrl = character.getString("image"); //we get the different info we want to display
                final String name = character.getString("name");
                final String status = character.getString("status");
                final String species = character.getString("species");
                final String gender = character.getString("gender");
                final String type = character.getString("type");
                Bitmap tmpBM = BitmapFactory.decodeStream((new URL(imageUrl)).openConnection().getInputStream());
                final Bitmap bmp = Bitmap.createScaledBitmap(tmpBM, 550, 550, true);
                final ImageView image = (ImageView) findViewById(R.id.imageViewEntry);
                image.post(new Runnable() {
                    public void run() {
                        image.setImageBitmap(bmp);
                    }
                });
                final TextView nameTV = (TextView) findViewById(R.id.textViewName);
                nameTV.post(new Runnable() {
                    @Override
                    public void run() {
                        nameTV.setText("name: " + name);
                    }
                });
                final ArrayList<String> entryInfos = new ArrayList<>(); // we fill the arraylist that will be used by the recyclerview containing the info
                entryInfos.add("status: " + status);
                entryInfos.add("species: " + species);
                if (!type.equals(""))// most characters don't have a "type" info
                    entryInfos.add("type: " + type);
                entryInfos.add("gender: " + gender);
                final RecyclerView recyclerView = findViewById(R.id.recyclerView);
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.setLayoutManager(new LinearLayoutManager(EntryActivity.this));
                        adapter = new MyRecyclerViewAdapter(EntryActivity.this, entryInfos);
                        recyclerView.setAdapter(adapter);
                    }
                });
                final Button next = (Button) findViewById(R.id.buttonNext); //button to display the next result from a search
                next.post(new Runnable() {
                    @Override
                    public void run() {
                        if (characters.size() < 2) { //if there is no next result, we disable the button
                            next.setEnabled(false);
                        }
                        else {
                            next.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    characters.remove(0); //we update the list of results
                                    Intent intent = new Intent(EntryActivity.this, EntryActivity.class);
                                    Log.i("meh", characters.get(0).toString());
                                    Gson gson = new Gson();
                                    String json = gson.toJson(characters);
                                    intent.putExtra("character", json);
                                    finish();
                                    startActivity(intent);
                                }
                            });
                        }
                    }
                });
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
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
