// MainActivity.java
package com.example.deteccaosonolencia;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyIconColor((Button) findViewById(R.id.btn_text_recognition), android.R.drawable.ic_menu_camera, android.R.color.white);
        applyIconColor((Button) findViewById(R.id.btn_face_detection), android.R.drawable.ic_menu_search, android.R.color.white);
        applyIconColor((Button) findViewById(R.id.btn_drowsiness_detection), android.R.drawable.ic_media_play, android.R.color.white);

    }


    private void applyIconColor(Button button, int drawableId, int colorId) {
        Drawable drawable = ContextCompat.getDrawable(this, drawableId);
        if (drawable != null) {
            drawable.setColorFilter(ContextCompat.getColor(this, colorId), PorterDuff.Mode.SRC_IN);
            button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
    }



    // Método para iniciar TextRecognitionActivity
    public void startTextRecognitionActivity(View view) {
        Intent intent = new Intent(this, TextRecognitionActivity.class);
        startActivity(intent);
    }
    public void startFaceDetectionActivity(View view) {
        Intent intent = new Intent(this, FaceDetectionActivity.class);
        startActivity(intent);
    }

    public void startDrowsinessDetectorActivity(View view) {
        Intent intent = new Intent(this, DrowsinessDetectorActivity.class);
        startActivity(intent);
    }


}
