package test.rit.harsh.kpt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    EditText review;
    private SentimentAnalysis nb;
    String[] testReviews;
    TextView poslabel, neglabel;
    public double[] score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        testReviews = new String[400];
        trainData t = new trainData();
        t.execute();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void senti(View view) {
        review = (EditText) findViewById(R.id.editText);
        String reviewText = review.getText().toString();
        classify c = new classify();
        c.execute(reviewText);
    }
    public void next(View view) {
        review = (EditText) findViewById(R.id.editText);
        Random r = new Random();
        int index = r.nextInt(399 - 0 + 1) + 0;
        review.setText("" + index);
        System.out.println(testReviews[index] + "," + index);
        review.setText(testReviews[index]);
    }

    public static String readFile(InputStream fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(fileName));
        String line="";
        StringBuilder sb = new StringBuilder();

        while ((line = br.readLine())!=null) {
            sb.append(line + " ");
            sb.append("\n");
        }
        return sb.toString();
    }

    private class classify extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()   {
            progressDialog= ProgressDialog.show(MainActivity.this, "Classifying","Geting Sentiment score", true);
        };
        @Override
        protected String doInBackground(String... params) {
            int label = 0;
            score = nb.classify(params[0]);

            double maxScore = score[0];
            for (int i = 0; i < score.length; i++) {
                if (score[i] > maxScore)
                    label = i;
            }


            System.out.println(label);
            String output;

            if (label == 0) {
                output = "Positive";
            } else {
                output = "Negative";
            }
            Snackbar.make(getCurrentFocus(), output, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            poslabel = (TextView)findViewById(R.id.poslabel);
            neglabel = (TextView) findViewById(R.id.neglabel);
            poslabel.setText(Double.toString(score[0]));
            neglabel.setText(Double.toString(score[1]));
        };
    }
    private class trainData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute()
        {
            progressDialog= ProgressDialog.show(MainActivity.this, "Training data","Please wait while the model is getting trained", true);
        };
        @Override
        protected String doInBackground(String... params) {
            try {
                nb = new SentimentAnalysis("train",getApplicationContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.err.println("traindata");
            AssetManager am = getAssets();

            try {
                String[] testPosFiles = getAssets().list("test"+File.separator+"pos");
                String[] testNegFiles = getAssets().list("test"+File.separator+"neg");

                String file;
                int position = 0;

                for (String fileEntry : testPosFiles) {
                    try {
                        InputStream fileName = am.open("test"+File.separator+"pos"+File.separator+fileEntry);
                        file = readFile(fileName);
                        testReviews[position++] = file;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                for (String fileEntry : testNegFiles) {
                    try {
                        InputStream fileName = am.open("test"+File.separator+"neg"+File.separator+fileEntry);
                        file = readFile(fileName);
                        testReviews[position++] = file;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            progressDialog.dismiss();
        };
    }
}
