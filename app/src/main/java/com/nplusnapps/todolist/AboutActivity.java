package com.nplusnapps.todolist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * The simple activity shows an additional information about the app.
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView aboutTextView = ((TextView) findViewById(R.id.text_about));
        aboutTextView.setText(Html.fromHtml(getString(R.string.app_about,
                getString(R.string.app_name), getString(R.string.app_version),
                getString(R.string.dev_email, getString(R.string.dev_name)))));
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}