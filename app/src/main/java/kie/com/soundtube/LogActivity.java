package kie.com.soundtube;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class LogActivity extends AppCompatActivity {

    HandlerThread thread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        String error = getIntent().getStringExtra("error message");
        error = "SoundTube version: " + BuildConfig.VERSION_CODE + "\n" + error;
        thread = new HandlerThread("worker");
        thread.start();
        final Handler worker = new Handler(thread.getLooper());
        final String finalError = error;
        worker.post(new Runnable() {
            @Override
            public void run() {
                Github github = new Github();
//                github.report(finalError);
                Log.d("LogActivity", finalError);
                finish();

            }

        });


    }

    @Override
    protected void onDestroy() {
        Log.d("LogActivity", "onDestroy");

        thread.quit();
        thread = null;
        super.onDestroy();
//        System.exit(0);

    }
}
