package com.rajesh.appbundle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;

public class MainActivity extends BaseActivity {

    private static final int CONFIRMATION_REQUEST_CODE = 1;
    private static final String PACKAGE_NAME = "com.rajesh.dynamic_feature";
    private static final String SAMPLE_CLASSNAME = "com.rajesh.dynamic_feature.SecondActivity";
    private static final String TAG = MainActivity.class.getSimpleName();
    private SplitInstallManager manager;
    private Group progress;
    private ProgressBar progressBar;
    private TextView progressText;
    private int sessionId;
    private Context context;


    private SplitInstallStateUpdatedListener listener = new SplitInstallStateUpdatedListener() {

        @Override
        public void onStateUpdate(SplitInstallSessionState state) {
            if (sessionId == state.sessionId()) {
                switch (state.status()) {
                    case SplitInstallSessionStatus.DOWNLOADING: {
                        //  In order to see this, the application has to be uploaded to the Play Store.
                        displayLoadingState(state, getString(R.string.downloading));
                    }
                    case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION: {
                /*
                  This may occur when attempting to download a sufficiently large module.
                  In order to see this, the application has to be uploaded to the Play Store.
                  Then features can be requested until the confirmation path is triggered.
                 */
                        try {
                            manager.startConfirmationDialogForResult(state, MainActivity.this, CONFIRMATION_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                    }
                    case SplitInstallSessionStatus.INSTALLED: {
                        try {
                            Context newContext = context.createPackageContext(context.getPackageName(), 0);
                            launchActivity(newContext);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                            toastAndLog(e.toString());
                        }
                    }

                    case SplitInstallSessionStatus.INSTALLING:
                        displayLoadingState(
                                state,
                                getString(R.string.installing)
                        );
                    case SplitInstallSessionStatus.FAILED: {
                        toastAndLog(getString(R.string.error_for_module, state.errorCode(),
                                state.moduleNames()));
                    }
                }
            } else {
                toastAndLog("Session didn't match");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        manager = SplitInstallManagerFactory.create(this);
        initializeViews();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadAndLaunchModule("dynamic_feature");
            }
        });
    }

    @Override
    protected void onResume() {
        manager.registerListener(listener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        manager.unregisterListener(listener);
        super.onPause();
    }

    private void initializeViews() {
        progress = findViewById(R.id.progress);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);
    }

    /**
     * Display a loading state to the user.
     */
    private void displayLoadingState(SplitInstallSessionState state, String message) {
        displayProgress();

        progressBar.setMax((int) state.totalBytesToDownload());
        progressBar.setProgress((int) state.bytesDownloaded());

        updateProgressMessage(message);
    }

    private void updateProgressMessage(String message) {
        if (progress.getVisibility() != View.VISIBLE) displayProgress();
        progressText.setText(message);
    }

    /**
     * Display progress bar and text.
     */
    private void displayProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CONFIRMATION_REQUEST_CODE) {
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            if (resultCode == Activity.RESULT_CANCELED) {
                toastAndLog(getString(R.string.user_cancelled));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void toastAndLog(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        Log.d(TAG, text);
    }

    private void loadAndLaunchModule(String name) {
        updateProgressMessage(getString(R.string.loading));
        // Skip loading if the module already is installed. Perform success action directly.
        if (manager.getInstalledModules().contains(name)) {
            updateProgressMessage(getString(R.string.already_installed));
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    launchActivity(MainActivity.this);
                }
            };
            handler.postDelayed(runnable, 5000);
            return;
        }

        // Create request to install a feature module by name.
        SplitInstallRequest request = SplitInstallRequest.newBuilder()
                .addModule(name)
                .build();

        // Load and install the requested feature module.
        manager.startInstall(request)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        if (e instanceof SplitInstallException) {
                            toastAndLog(((SplitInstallException) e).getErrorCode() + "");
                        }
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        sessionId = integer;
                    }
                });

        updateProgressMessage(getString(R.string.starting_install_for));
    }

    /**
     * Launch an activity by its class name.
     *
     * @param newContext
     */
    private void launchActivity(Context newContext) {
        Intent intent = new Intent().setClassName(BuildConfig.APPLICATION_ID, MainActivity.SAMPLE_CLASSNAME);
        startActivity(intent);
    }
}
