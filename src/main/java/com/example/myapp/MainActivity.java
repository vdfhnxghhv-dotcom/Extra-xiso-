package com.example.myapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private EditText etInputPath, etOutputPath;
    private Button btnExecute, btnBrowseInput, btnBrowseOutput;
    private TextView tvStatus, tvInputLabel, tvOutputLabel;
    private RadioGroup radioGroupOperation;
    private RadioButton radioExtract, radioCreate;
    private static final int REQ_INPUT_FILE = 1;
    private static final int REQ_OUTPUT_DIR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etInputPath = findViewById(R.id.etInputPath);
        etOutputPath = findViewById(R.id.etOutputPath);
        btnExecute = findViewById(R.id.btnExecute);
        btnBrowseInput = findViewById(R.id.btnBrowseInput);
        btnBrowseOutput = findViewById(R.id.btnBrowseOutput);
        tvStatus = findViewById(R.id.tvStatus);
        tvInputLabel = findViewById(R.id.tvInputLabel);
        tvOutputLabel = findViewById(R.id.tvOutputLabel);
        radioGroupOperation = findViewById(R.id.radioGroupOperation);
        radioExtract = findViewById(R.id.radioExtract);
        radioCreate = findViewById(R.id.radioCreate);

        try {
            checkStoragePermission();
        } catch (Exception e) {
            tvStatus.setText("Permission Error: " + e.getMessage());
        }

        radioGroupOperation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioExtract) {
                    tvInputLabel.setText("Input ISO Path:");
                    etInputPath.setHint("/storage/emulated/0/Download/game.iso");
                } else {
                    tvInputLabel.setText("Source Folder Path:");
                    etInputPath.setHint("/storage/emulated/0/Download/game_folder");
                }
            }
        });

        btnBrowseInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (radioCreate.isChecked()) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, REQ_INPUT_FILE);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    startActivityForResult(intent, REQ_INPUT_FILE);
                }
            }
        });

        btnBrowseOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, REQ_OUTPUT_DIR);
            }
        });

        btnExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etInputPath.getText().toString().trim();
                String output = etOutputPath.getText().toString().trim();

                if (input.isEmpty() || output.isEmpty()) {
                    tvStatus.setText("Error: Paths cannot be empty.");
                    return;
                }

                btnExecute.setEnabled(false);
                tvStatus.setText("Status: Processing...");
                boolean isExtract = radioExtract.isChecked();
                executeOperation(input, output, isExtract);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == REQ_INPUT_FILE) {
                    if (radioCreate.isChecked()) {
                        String realPath = getPathFromUri(uri);
                        if (realPath != null) etInputPath.setText(realPath);
                        else Toast.makeText(this, "Please select a folder", Toast.LENGTH_SHORT).show();
                    } else {
                        String tempPath = copyToCache(uri);
                        if (tempPath != null) etInputPath.setText(tempPath);
                        else Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == REQ_OUTPUT_DIR) {
                    String realPath = getPathFromUri(uri);
                    if (realPath != null) etOutputPath.setText(realPath);
                    else Toast.makeText(this, "Please type output path manually", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // دالة لجلب الاسم الحقيقي للملف من مدير الملفات
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // تحديث دالة النسخ للاحتفاظ بالاسم الأصلي
    private String copyToCache(Uri uri) {
        try {
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "input_temp.iso";
            }
            // تنظيف الاسم من أي رموز غريبة
            fileName = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");

            InputStream in = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.close();
            in.close();
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPathFromUri(Uri uri) {
        try {
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                String docId;
                if (uri.getPath().contains("/tree/")) {
                    docId = DocumentsContract.getTreeDocumentId(uri);
                } else {
                    docId = DocumentsContract.getDocumentId(uri);
                }
                String[] split = docId.split(":");
                String type = split[0];
                String path = split.length > 1 ? split[1] : "";
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + path;
                } else {
                    return "/storage/" + type + "/" + path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Uri uri = Uri.parse("package:" + getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 100);
            }
        }
    }

    private void executeOperation(final String inputPath, final String outputPath, final boolean isExtract) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String binaryPath = getApplicationInfo().nativeLibraryDir + "/libextract-xiso.so";
                    File binaryFile = new File(binaryPath);

                    if (!binaryFile.exists()) {
                        throw new Exception("Binary not found at: " + binaryPath);
                    }

                    ProcessBuilder pb;
                    
                    if (isExtract) {
                        // --- التسمية التلقائية للاستخراج ---
                        File inputFile = new File(inputPath);
                        String fileName = inputFile.getName();
                        // إزالة الامتداد .iso للحصول على اسم المجلد
                        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                        
                        // إنشاء المجلد داخل مسار الإخراج المحدد
                        File targetDir = new File(outputPath, baseName);
                        if (!targetDir.exists()) targetDir.mkdirs();
                        
                        pb = new ProcessBuilder(binaryFile.getAbsolutePath(), "-x", inputPath);
                        pb.directory(targetDir); 
                    } else {
                        // --- التسمية التلقائية للإنشاء ---
                        File inputDir = new File(inputPath);
                        String folderName = inputDir.getName();
                        
                        // إضافة .iso إلى اسم المجلد
                        File outputFile = new File(outputPath, folderName + ".iso");
                        File outputParent = outputFile.getParentFile();
                        if (outputParent != null && !outputParent.exists()) {
                            outputParent.mkdirs();
                        }
                        
                        pb = new ProcessBuilder(binaryFile.getAbsolutePath(), "-c", inputPath, outputFile.getAbsolutePath());
                        pb.directory(outputParent);
                    }

                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    final StringBuilder outputLog = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        outputLog.append(line).append("\n");
                    }

                    final int exitCode = process.waitFor();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnExecute.setEnabled(true);
                            if (exitCode == 0) {
                                tvStatus.setText("Status: Success!\n\n" + outputLog.toString());
                            } else {
                                tvStatus.setText("Status: Failed (Code " + exitCode + ")\n\n" + outputLog.toString());
                            }
                        }
                    });

                } catch (Exception e) {
                    final String errorMsg = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnExecute.setEnabled(true);
                            tvStatus.setText("Exception: " + errorMsg);
                        }
                    });
                }
            }
        }).start();
    }
}