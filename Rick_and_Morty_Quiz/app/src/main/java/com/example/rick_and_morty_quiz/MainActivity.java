package com.example.rick_and_morty_quiz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private HttpConnectionTask httpConnectionTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) { //searchbar fragment call
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new DynamicFragmentSearchBar()).commit();
        }
        if (httpConnectionTask != null)
            httpConnectionTask.cancel(true);
        httpConnectionTask = new HttpConnectionTask();
        httpConnectionTask.execute(); //main asyncTask call
        Button buttonOtherChar = (Button) findViewById(R.id.buttonOther);//button to change the random character (by reopening the activity)
        buttonOtherChar.setOnClickListener(new View.OnClickListener() { //button to get a new random
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }
        });
    }

    private class HttpConnectionTask extends AsyncTask<Void, Void, String> {

        String partialAnswer;
        int currentIndex = 0;
        Intent intent = new Intent(MainActivity.this, EntryActivity.class);

        @Override
        protected String doInBackground(Void... voids) {
            String result = null;
            try {
                final Random r = new Random();
                int page = r.nextInt(24 + 1) + 1; //random character page number
                String urlStr = "https://rickandmortyapi.com/api/character/";
                if (page != 1) //first page url does not end the same as others
                    urlStr += "?page=" + page;
                URL url = new URL(urlStr);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    result = readStream(in);
                    JSONObject json = new JSONObject(result); //object containing a full character page chosen randomly
                    int id = 0; //id % 20 of a random character, 20 being the number of characters per page
                    if (page != 25) //last page contains only 13 characters
                        id = r.nextInt(18 + 1);
                    else
                        id = r.nextInt(11 + 1);
                    final int idFinal = id; //simple copy to use in UI thread
                    final JSONArray characters = json.getJSONArray("results");
                    final TextView answer = (TextView) findViewById(R.id.textViewAnswer);
                    final String name = characters.getJSONObject(id).getString("name"); //name of the character
                    answer.post(new Runnable() {
                        public void run() { //we use the UI thread
                            String answerStr = "";
                            int letterCount = 0; //will contain the number of letters in the name (not special chars nor numbers)
                            for (int i = 0; i < name.length(); ++i) {
                                if (Character.isLetter(name.charAt(i))) {
                                    answerStr += "_ ";
                                    ++letterCount;
                                }
                                else
                                    answerStr += name.charAt(i) + " ";
                            }
                            if (letterCount > 25)// due to most names being LT 25 letters long and letter buttons covering the GUI already, I chose to limit max handled length to 25
                                Toast.makeText(getApplicationContext(),"/!\\ NAME TOO LONG, NO SOLUTION /!\\", Toast.LENGTH_LONG).show();
                            answer.setText(answerStr);// name hidden in format _ _ _ _ _ _, will update as solution is found
                            partialAnswer = answerStr;
                        }
                    });
                    final TableLayout tl = (TableLayout) findViewById(R.id.tableLayout);
                    tl.post(new Runnable() {
                        @Override
                        public void run() {
                            String fillerName = "";//character name used to fill the letter buttons
                            for (int i = 0; i < name.length(); ++i) {
                                if (Character.isLetter(name.charAt(i))) {
                                    fillerName += name.charAt(i);
                                }
                            }
                            fillerName = fillerName.toUpperCase();
                            for (int l = fillerName.length(); l < 25; ++l) { // if name too short to fill all buttons, we append random other letters
                                int letterIndex = r.nextInt(25 + 1) + 65;
                                fillerName += (char) letterIndex;
                            }
                            Log.i("meh", fillerName); //for testing purposes
                            TableRow tr;
                            Button letterButton;
                            int fillerIndex;
                            for (int i = 0; i < 5; ++i) {//for button rows
                                tr = (TableRow) tl.getChildAt(i);
                                for (int j = 0; j < 5; ++j) {//for button columns
                                    letterButton = (Button) tr.getChildAt(j);
                                    if (i == 4 && j == 4)
                                        fillerIndex = 0;
                                    else
                                        fillerIndex = (r.nextInt((24 - (i * 5 + j + 1)) + 1));//buttons are filled in order, but using random letter from fillername
                                    letterButton.setText("" + fillerName.charAt(fillerIndex));
                                    fillerName = fillerName.substring(0,fillerIndex) +  fillerName.substring(fillerIndex + 1);//fillername needs to be updated so that we don't use the same letters twice
                                }
                            }
                        }
                    });
                    String imageUrl = characters.getJSONObject(id).getString("image"); //we get the url of the image
                    Log.i("meh", imageUrl);
                    try {
                        Bitmap tmpBM = BitmapFactory.decodeStream((new URL(imageUrl)).openConnection().getInputStream());
                        final Bitmap bmp = Bitmap.createScaledBitmap(tmpBM, 550, 550, true);//we scale the image (originally too small)
                        final ImageView image = (ImageView) findViewById(R.id.imageView);
                        image.post(new Runnable() {
                            public void run() {
                                image.setImageBitmap(bmp);// we display the image from the url
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    final View.OnClickListener lettersClicked = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { // listener for letter buttons
                            Button btn = (Button) v;
                            if (currentIndex >= name.length()) {// if name is complete
                                try {
                                    Log.i("meh", characters.getJSONObject(idFinal).toString());//for testing purposes
                                    ArrayList<String> character = new ArrayList<>();//quite useless as is, needed for proper handling of search bar
                                    character.add(characters.getJSONObject(idFinal).toString());
                                    Gson gson = new Gson();
                                    String json = gson.toJson(character);
                                    intent.putExtra("character", json);//we send an array with 1 entry, because the other activity can handle more entries from search bar
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                finish();
                                startActivity(intent);
                            }
                            if (btn.getText().charAt(0) == Character.toUpperCase(name.charAt(currentIndex))) {//if user touched one of the right buttons (if current first missing letter is the one pressed)
                                btn.setEnabled(false);
                                if (currentIndex == name.length() - 1) { //ending, same as above due to unexplained corner case
                                    try {
                                        Log.i("meh", characters.getJSONObject(idFinal).toString());
                                        ArrayList<String> character = new ArrayList<>();
                                        character.add(characters.getJSONObject(idFinal).toString());
                                        Gson gson = new Gson();
                                        String json = gson.toJson(character);
                                        intent.putExtra("character", json);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finish();
                                    startActivity(intent);
                                }
                                TextView answer = (TextView) findViewById(R.id.textViewAnswer);
                                partialAnswer = partialAnswer.substring(0, currentIndex * 2) + btn.getText().charAt(0) + partialAnswer.substring(currentIndex * 2 + 1);//answer updated
                                answer.setText(partialAnswer);
                                currentIndex++;//next 7 lines are for incrementing the index, we skip non-letter chars
                                Character c = '&';
                                if (currentIndex < name.length())
                                    c = name.charAt(currentIndex);
                                while (currentIndex < name.length() && (c < 65 || (c > 90 && (c < 97 || c > 122)))) {
                                    currentIndex++;
                                    if (currentIndex < name.length())
                                        c = name.charAt(currentIndex);
                                }
                            }
                        }
                    };
                    tl.post(new Runnable() {
                        @Override
                        public void run() {
                            TableRow tr;
                            for (int i = 0; i < 5; ++i) {//setting OnClickListener to each letter button
                                tr = (TableRow) tl.getChildAt(i);
                                for (int j = 0; j < 5; ++j) {
                                    Button btn = (Button) tr.getChildAt(j);
                                    btn.setOnClickListener(lettersClicked);
                                }
                            }
                        }
                    });
                    final Button giveUp = (Button) findViewById(R.id.buttonGiveUp); // button to give up and get the character info instantly
                    giveUp.post(new Runnable() {
                        @Override
                        public void run() {
                            giveUp.setOnClickListener(new View.OnClickListener() {//button to give up, redirects just like finding the full name
                                @Override
                                public void onClick(View v) {
                                    try {
                                        Log.i("meh", characters.getJSONObject(idFinal).toString());
                                        ArrayList<String> character = new ArrayList<>();
                                        character.add(characters.getJSONObject(idFinal).toString());
                                        Gson gson = new Gson();
                                        String json = gson.toJson(character);
                                        intent.putExtra("character", json);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    finish();
                                    startActivity(intent);
                                }
                            });
                        }
                    });
                }catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {

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
