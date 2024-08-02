package com.example.googledriveapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_AUTHORIZATION = 1001;
    private GoogleSignInClient googleSignInClient;
    private Drive googleDriveService;
    private ProgressBar loadingProgressBar;
    private RecyclerView fileRecyclerView;
    private FileAdapter fileAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        fileRecyclerView = findViewById(R.id.file_recycler_view);

        Button logoutButton = findViewById(R.id.logout_button);


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            initializeDriveService(account);
            listFiles();
        } else {
            Log.e(TAG, "No account signed in");
        }

        logoutButton.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    // Log out işlemi başarılı, LoginActivity'ye yönlendirme yap
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // else {
                    Toast.makeText(MainActivity.this, "Log out failed", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Log out failed", task.getException());
                }
            });
        });
    }



    private void initializeDriveService(GoogleSignInAccount account) {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            googleDriveService = new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("Your Application Name")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Error initializing Drive service", e);
        }
    }

    private void listFiles() {
        new Thread(() -> {
            try {
                FileList result = googleDriveService.files().list()
                        .setPageSize(10)
                        .setFields("nextPageToken, files(id, name)")
                        .execute();
                List<File> files = result.getFiles();
                runOnUiThread(() -> {
                    if (files == null || files.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No files found.", Toast.LENGTH_SHORT).show();
                    } else {
                        fileAdapter = new FileAdapter(files, new FileAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(File file) {
                                // item click action
                            }

                            @Override
                            public void onDownloadClick(File file) {
                                downloadFile(file);
                            }

                            @Override
                            public void onDeleteClick(File file) {
                                deleteFile(file);
                            }

                            @Override
                            public void onEditClick(File file) {
                                // edit action
                            }
                        });
                        fileRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                        fileRecyclerView.setAdapter(fileAdapter);
                        fileRecyclerView.setVisibility(View.VISIBLE);
                    }
                    loadingProgressBar.setVisibility(View.GONE);
                });
            } catch (UserRecoverableAuthIOException e) {
                Log.e(TAG, "User needs to authorize access", e);
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e(TAG, "Unable to list files", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingProgressBar.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void downloadFile(File file) {
        new Thread(() -> {
            try {
                String fileId = file.getId();
                java.io.File filePath = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName());
                try (OutputStream outputStream = new FileOutputStream(filePath)) {
                    googleDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "File downloaded: " + filePath.getAbsolutePath(), Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Error downloading file", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error downloading file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void deleteFile(File file) {
        new Thread(() -> {
            try {
                googleDriveService.files().delete(file.getId()).execute();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "File deleted: " + file.getName(), Toast.LENGTH_SHORT).show();
                    // Update the file list
                    listFiles();
                });
            } catch (IOException e) {
                Log.e(TAG, "Error deleting file", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error deleting file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK) {
                listFiles();
            } else {
                Log.e(TAG, "Authorization failed");
            }
        }
    }
}
