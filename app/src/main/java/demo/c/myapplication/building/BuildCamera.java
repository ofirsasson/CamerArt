package demo.c.myapplication.building;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import demo.c.myapplication.MainActivity;
import demo.c.myapplication.R;
import demo.c.myapplication.filming.BaseCameraActivity;
import demo.c.myapplication.filming.OverlayView;
import ai.fritz.core.utils.BitmapUtils;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.imagesegmentation.FritzVisionSegmentResult;
import butterknife.ButterKnife;


public class BuildCamera extends BaseCameraActivity implements ImageReader.OnImageAvailableListener {
    private static final String TAG = BuildCamera.class.getSimpleName();
    private static final String PATH = Environment.getExternalStorageDirectory().toString() + "/Styled photos/";
    //upload from photo vars
    private static final Size DESIRED_PREVIEW_SIZE = new Size(400, 600);
    private AtomicBoolean computing = new AtomicBoolean(false);
    private int imgRotation;
    private FritzVisionSegmentResult segmentResult;
    private FritzVisionImage visionImage;
    Button snapshotButton;
    RelativeLayout previewLayout;
    RelativeLayout snapshotLayout;
    OverlayView snapshotOverlay;
    ProgressBar spinner;
    Button checkbtn;
    Button closeButton;
    Bitmap tempforfile;
    //upload from gallery vars
    private static final int PICK_FILE_REQUEST = 1;
    private String selectedFilePath;
    private String SERVER_URL = "your/ftp/server";
    ProgressDialog dialog;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.buildcamera;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    //upload from camera functions
    @Override
    public void onPreviewSizeChosen(final Size size, final Size cameraSize, final int rotation) {
        //upload from photo vars
        imgRotation = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        snapshotButton = findViewById(R.id.take_picture_btn);
        previewLayout = findViewById(R.id.preview_frame);
        snapshotLayout = findViewById(R.id.snapshot_frame);
        snapshotOverlay = findViewById(R.id.snapshot_view);
        closeButton = findViewById(R.id.close_btn);
        spinner = findViewById(R.id.spinner);
        checkbtn = findViewById(R.id.checkbutton);


        if (snapshotOverlay != null) {
            snapshotOverlay.addCallback(new OverlayView.DrawCallback() {
                @Override
                public void drawCallback(final Canvas canvas) {
                    if (segmentResult != null) {
                        segmentResult.drawVisionImage(canvas, cameraSize);
                        segmentResult.drawAllMasks(canvas, 100, cameraSize);
                    }
                }
            });
        }

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (visionImage != null) {
                            Bitmap scaledBitmap = BitmapUtils.scale(visionImage.rotateBitmap(), cameraSize.getWidth(), cameraSize.getHeight());
                            canvas.drawBitmap(scaledBitmap, new Matrix(), new Paint());
                        }
                    }
                });
        myaddCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (visionImage != null) {
                            tempforfile = BitmapUtils.scale(visionImage.rotateBitmap(), cameraSize.getWidth(), cameraSize.getHeight());
                            canvas.drawBitmap(tempforfile, new Matrix(), new Paint());
                        }
                    }
                });

        snapshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                runInBackground(
                        new Runnable() {
                            @Override
                            public void run() {
                                showSpinner();
                                showSnapshotLayout();
                                hideSpinner();
                                snapshotOverlay.postInvalidate();
                                computing.set(false);
                            }
                        });

            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPreviewLayout();
            }
        });
        checkbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endofsave();
                dialog = ProgressDialog.show(BuildCamera.this,"","Uploading File...",true);
                new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //creating new thread to handle Http Operations
                            uploadFile(PATH + "TempImage-" + MainActivity.account.getDisplayName() +".jpg");
                        }
                    }).start();
                }

        });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void showSnapshotLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previewLayout.setVisibility(View.GONE);
                snapshotLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPreviewLayout() {
        previewLayout.setVisibility(View.VISIBLE);
        snapshotLayout.setVisibility(View.GONE);
    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (computing.get()) {
            image.close();
            requestRender();
            return;

        }

        visionImage = FritzVisionImage.fromMediaImage(image, imgRotation);
        image.close();

        requestRender();
    }

    private void endofsave() {
        String root = PATH;
        File myDir = new File(root);
        myDir.mkdir();
        String fname = "TempImage.jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            tempforfile.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this,
                "Photo uploaded!", Toast.LENGTH_LONG).show();
        showPreviewLayout();
    }



    //android upload file to server
    public int uploadFile(final String selectedFilePath) {

        int serverResponseCode = 0;

        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";


        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);


        String[] parts = selectedFilePath.split("/");
        final String fileName = parts[parts.length - 1];
        dialog.dismiss();
        if (selectedFile.isFile()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(SERVER_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", selectedFilePath);

                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);

                dataOutputStream.writeBytes(lineEnd);

                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);


                selectedFile.delete();
                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BuildCamera.this, "File Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(BuildCamera.this, "URL error!", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(BuildCamera.this, "Cannot Read/Write File!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            return serverResponseCode;
        }
        return 0;
    }
}


