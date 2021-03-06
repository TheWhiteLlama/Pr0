package com.pr0gramm.app.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.pr0gramm.app.R;
import com.pr0gramm.app.SyncBroadcastReceiver;
import com.pr0gramm.app.api.pr0gramm.response.Login;
import com.pr0gramm.app.services.UserService;

import net.danlew.android.joda.DateUtils;

import org.joda.time.DateTimeZone;
import org.joda.time.Weeks;

import javax.inject.Inject;

import retrofit.RetrofitError;
import roboguice.RoboGuice;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.InjectView;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.internal.operators.OperatorMap;

import static com.pr0gramm.app.AndroidUtility.toObservable;
import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.defaultOnError;
import static com.pr0gramm.app.ui.dialogs.ErrorDialogFragment.showErrorString;
import static com.pr0gramm.app.ui.fragments.BusyDialogFragment.busyDialog;
import static rx.android.app.AppObservable.bindActivity;

/**
 */
public class LoginActivity extends RoboActionBarActivity {
    private static final String PREF_USERNAME = "LoginDialogFragment.username";

    @Inject
    private SharedPreferences prefs;

    @Inject
    private UserService userService;

    @InjectView(R.id.username)
    private EditText usernameView;

    @InjectView(R.id.password)
    private EditText passwordView;

    @InjectView(R.id.login)
    private Button submitView;

    private Subscription subscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // restore last username
        String defaultUsername = prefs.getString(PREF_USERNAME, "");
        if (!Strings.isNullOrEmpty(defaultUsername)) {
            usernameView.setText(defaultUsername);
        }

        submitView.setOnClickListener(v -> onLoginClicked());
    }

    private void enableView(boolean enable) {
        for (View view : ImmutableList.<View>of(usernameView, passwordView, submitView)) {
            view.setEnabled(enable);
        }
    }

    private void onLoginClicked() {
        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();

        if (username.isEmpty()) {
            usernameView.setError(getString(R.string.must_not_be_empty));
            return;
        }

        if (password.isEmpty()) {
            passwordView.setError(getString(R.string.must_not_be_empty));
            return;
        }

        enableView(false);

        // store last username
        prefs.edit().putString(PREF_USERNAME, username).apply();

        subscription = bindActivity(this, userService.login(username, password))
                .lift(busyDialog(this, getString(R.string.login_please_wait), UserService.LoginProgress::getProgress))
                .flatMap(progress -> toObservable(progress.getLogin()))
                .lift(new LoginErrorInterceptor())
                .doOnError(err -> enableView(true))
                .subscribe(this::onLoginResponse, defaultOnError());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscription != null)
            subscription.unsubscribe();
    }

    private static class LoginErrorInterceptor implements Observable.Operator<Login, Login> {
        @Override
        public Subscriber<? super Login> call(Subscriber<? super Login> subscriber) {
            return new Subscriber<Login>() {
                @Override
                public void onCompleted() {
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable err_) {
                    if (err_ instanceof RetrofitError) {
                        RetrofitError err = (RetrofitError) err_;
                        if (err.getResponse() != null && err.getResponse().getStatus() == 403) {
                            try {
                                subscriber.onNext(new Login(false));
                                subscriber.onCompleted();

                            } catch (Throwable forward) {
                                subscriber.onError(forward);
                            }

                            return;
                        }
                    }
                    subscriber.onError(err_);
                }

                @Override
                public void onNext(Login value) {
                    subscriber.onNext(value);
                }
            };
        }
    }

    private void onLoginResponse(Login response) {
        if (response.isSuccess()) {
            SyncBroadcastReceiver.scheduleNextSync(this);

            // signal success
            setResult(RESULT_OK);
            finish();

        } else {
            Login.BanInfo ban = response.getBan();
            if (ban != null && ban.isBanned()) {
                CharSequence date = DateUtils.getRelativeDateTimeString(this,
                        ban.getTill().toDateTime(DateTimeZone.getDefault()),
                        Weeks.ONE,
                        DateUtils.FORMAT_SHOW_DATE);

                String reason = ban.getReason();
                showErrorString(getSupportFragmentManager(), getString(R.string.banned, date, reason));

            } else {
                String msg = getString(R.string.login_not_successful);
                showErrorString(getSupportFragmentManager(), msg);
                enableView(true);
            }
        }
    }

    public static abstract class DoIfAuthorizedHelper {
        private final int REQUEST_CODE = 8413;

        @Nullable
        private Runnable retry;

        private DoIfAuthorizedHelper() {
        }

        public void onActivityResult(int requestCode, int resultCode) {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == RESULT_OK && retry != null) {
                    retry.run();
                }

                retry = null;
            }
        }

        /**
         * Executes the given runnable if a user is signed in. If not, this method
         * will show a login screen.
         */
        public boolean run(Runnable runnable) {
            return run(runnable, null);
        }

        /**
         * Executes the given runnable if a user is signed in. If not, this method shows
         * the login screen. After a successful login, the given 'retry' runnable will be called.
         */
        public boolean run(Runnable runnable, @Nullable Runnable retry) {
            Context context = getContext();
            if (context == null)
                return false;

            UserService userService = RoboGuice
                    .getInjector(context)
                    .getInstance(UserService.class);

            if (userService.isAuthorized()) {
                runnable.run();
                return true;

            } else {
                this.retry = retry;

                Intent intent = new Intent(context, LoginActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                return false;
            }
        }

        protected abstract Context getContext();

        protected abstract void startActivityForResult(Intent intent, int requestCode);
    }

    public static DoIfAuthorizedHelper helper(Activity activity) {
        return new DoIfAuthorizedHelper() {
            @Override
            protected Context getContext() {
                return activity;
            }

            @Override
            protected void startActivityForResult(Intent intent, int requestCode) {
                activity.startActivityForResult(intent, requestCode);
            }
        };
    }

    public static DoIfAuthorizedHelper helper(Fragment fragment) {
        return new DoIfAuthorizedHelper() {
            @Override
            protected Context getContext() {
                return fragment.getActivity();
            }

            @Override
            protected void startActivityForResult(Intent intent, int requestCode) {
                fragment.startActivityForResult(intent, requestCode);
            }
        };
    }
}
