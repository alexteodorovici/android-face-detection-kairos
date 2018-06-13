package com.ideeastudios.facekairos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.kairos.Kairos;
import com.kairos.KairosListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    public Kairos myKairos = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* * * instantiate a new kairos instance * * */
        myKairos = new Kairos();

        /* * * set authentication * * */

        String app_id = "YOUR_APP_ID";
        String api_key = "YOUR_API_KEY";

        myKairos.setAuthentication(this, app_id, api_key);

        //pick a photo button
        Button button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setText("");
                //start an intent to pick an image
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

        //instantiate the progress dialog for Kairos api call
        detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            //process the chosen image
            Uri uri = data.getData();
            try {
                final Bitmap[] bitmap = {MediaStore.Images.Media.getBitmap(getContentResolver(), uri)};
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap[0]);

                // Kairos face detection listener
                KairosListener listener = new KairosListener() {
                    @Override
                    public void onSuccess(String response) {
                        detectionProgressDialog.dismiss();
                        Log.d("KAIROS DEMO", response);

                        try {
                            //parse the response from Kairos
                            JSONObject json = new JSONObject(response);
                            JSONArray images = json.getJSONArray("images");
                            for (int i = 0; i < images.length(); i++) {
                                JSONObject facesObj = images.getJSONObject(i);
                                Log.d("FACETEST", "facesObj " + facesObj.toString());

                                JSONArray facesArray = facesObj.getJSONArray("faces");
                                for (int k = 0; k < facesArray.length(); k++) {
                                    JSONObject face = facesArray.getJSONObject(k);
                                    JSONObject attributes = face.getJSONObject("attributes");

                                    double age = attributes.optDouble("age", 0);

                                    JSONObject gender = attributes.getJSONObject("gender");
                                    String genderStr = gender.optString("type", "unknown");

                                    double width = face.optDouble("width", 0);
                                    double height = face.optDouble("height", 0);
                                    double topLeftX = face.optDouble("topLeftX", 0);
                                    double topLeftY = face.optDouble("topLeftY", 0);

                                    Log.d("FACETEST", "FACE " + k + " age: " + age + " gender : " + genderStr);
                                    TextView textView = (TextView) findViewById(R.id.textView);
                                    textView.setText(textView.getText() + "FACE " + k + " age: " + age + " gender : " + genderStr + " \n");

                                    bitmap[0] = drawFaceRectanglesOnBitmap(bitmap[0], (float) width, (float) height, (float) topLeftX, (float) topLeftY);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFail(String response) {
                        detectionProgressDialog.dismiss();
                        Log.d("KAIROS DEMO", response);
                    }
                };

                //some Kairos settings
                String selector = "FULL";
                String minHeadScale = "0.015";

                //start the progress dialog while Kairos runs the face detection process
                detectionProgressDialog.setMessage("Please wait while detecting faces...");
                detectionProgressDialog.show();
                myKairos.detect(bitmap[0], selector, minHeadScale, listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, float width, float height, float topLeftX, float topLeftY) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        int stokeWidth = 10;
        paint.setStrokeWidth(stokeWidth);

        canvas.drawRect(topLeftX, topLeftY, topLeftX + width, topLeftY + height, paint);

        ImageView imageView = (ImageView) findViewById(R.id.imageView1);
        imageView.setImageBitmap(bitmap);
        return bitmap;
    }


}
