package com.pr0gramm.app.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.pr0gramm.app.DialogBuilder;
import com.pr0gramm.app.R;
import com.pr0gramm.app.services.FeedbackService;
import com.pr0gramm.app.services.UserService;

import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;
import rx.functions.Actions;

import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.defaultOnError;
import static com.pr0gramm.app.ui.fragments.BusyDialogFragment.busyDialog;
import static rx.android.app.AppObservable.bindActivity;

/**
 */
public class FeedbackActivity extends RoboActionBarActivity {
    @Inject
    private FeedbackService feedbackService;

    @Inject
    private UserService userService;

    @InjectView(R.id.submit)
    private Button buttonSubmit;

    @InjectView(R.id.feedback_name)
    private EditText vName;

    @InjectView(R.id.feedback_text)
    private EditText vText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        int primary = getResources().getColor(R.color.primary);
        ViewCompat.setBackgroundTintList(buttonSubmit, ColorStateList.valueOf(primary));
        buttonSubmit.setOnClickListener(v -> submitClicked());

        vText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean empty = s.toString().trim().isEmpty();
                buttonSubmit.setEnabled(!empty);
            }
        });

        Optional<String> name = userService.getName();
        if (name.isPresent()) {
            vName.setText(name.get());
        }
    }

    private void submitClicked() {
        String name = vName.getText().toString().trim();
        String feedback = vText.getText().toString().trim();

        bindActivity(this, feedbackService.post(name, feedback))
                .lift(busyDialog(this))
                .doOnCompleted(this::onSubmitSuccess)
                .subscribe(Actions.empty(), defaultOnError());
    }

    private void onSubmitSuccess() {
        DialogBuilder.start(this)
                .content(R.string.feedback_sent)
                .positive(R.string.okay, di -> finish())
                .onCancel(di -> finish())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
