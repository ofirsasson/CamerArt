package demo.c.myapplication.filming;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.fritz.core.FritzOnDeviceModel;
import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.styletransfer.FritzStyleResolution;
import ai.fritz.vision.styletransfer.FritzVisionStylePredictor;
import ai.fritz.vision.styletransfer.FritzVisionStylePredictorOptions;
import ai.fritz.vision.styletransfer.FritzVisionStyleResult;
import demo.c.myapplication.R;


public class StyleTransferActivity extends BaseCameraActivity implements OnImageAvailableListener {
    private static final String TAG = StyleTransferActivity.class.getSimpleName();
    //TF
    private static final int WANTED_WIDTH = 300;//420;
    private static final int WANTED_HEIGHT = 400;//560;
    private static final String MODEL_FILE = "file:///android_asset/bicentennial_print_640x480_015_small_optimized.pb";
    private static final String INPUT_NODE = "img_placeholder";
    private static final String OUTPUT_NODE = "preds";
    private TensorFlowInferenceInterface mInferenceInterface;
    // Should this activity show the custom style transfer model (cycles through included models if false)
    private static final Size DESIRED_PREVIEW_SIZE = new Size(480, 640);
    private static final String[] STYLE_NAMES = {
            "BICENTENNIAL_PRINT",
            "FEMMES",
            "HEAD_OF_CLOWN",
            "HORSES_ON_SEASHORE",
            "KALEIDOSCOPE",
            "PINK_BLUE_RHOMBUS",
            "POPPY_FIELD",
            "RITMO_PLASTICO",
            "STARRY_NIGHT",
            "THE_SCREAM",
            "THE_TRAIL"

    };

    private AtomicBoolean computing = new AtomicBoolean(false);

    private FritzVisionImage styledImage;
    private FritzVisionStylePredictor predictor;
    private FritzVisionStylePredictorOptions options;

    private Size cameraViewSize;
    private OverlayView overlayView;
    private OverlayView snapshotView;
    private int activeStyleIndex = 0;

    private int imageRotationFromCamera;

    private FritzVisionStyleResult styleResult;

    Button snapshotButton;
    RelativeLayout snapshotLayout;
    ProgressBar spinner;
    Button closeButton;
    Button savebtn;

    Bitmap outbitmap;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_stylize;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onPreviewSizeChosen(final Size previewSize, final Size cameraViewSize, final int rotation) {

        snapshotButton = findViewById(R.id.take_picture_btn);
        snapshotLayout = findViewById(R.id.snapshot_frame);
        closeButton = findViewById(R.id.close_btn);
        spinner = findViewById(R.id.spinner);
        savebtn = findViewById(R.id.checkbutton);
        showSpinner();
        options = new FritzVisionStylePredictorOptions.Builder()
                .imageResolution(FritzStyleResolution.HIGH)
                .build();
        assignPredictor();

        imageRotationFromCamera = FritzVisionOrientation.getImageRotationFromCamera(this, cameraId);
        this.cameraViewSize = cameraViewSize;

        overlayView = findViewById(R.id.debug_overlay);
        snapshotView = findViewById(R.id.snapshot_view);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (styleResult != null) {
                            styleResult.drawToCanvas(canvas, cameraViewSize);
                        }
                    }
                });
        myaddCallback(new OverlayView.DrawCallback() {
            @Override
            public void drawCallback(Canvas canvas) {
                if (styleResult != null) {
                    styleResult.drawToCanvas(canvas, cameraViewSize);
                }
            }
        });

        // Don't add a click handler if the activity is showing a custom style transfer model.
        overlayView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNextPredictor();
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
                                requestRender();
                                showSnapshotLayout();
                                hideSpinner();
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
        savebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endofsave();
            }
        });
    }

    private void getNextPredictor() {
        FritzOnDeviceModel[] styles = PaintingStyles.getAll();

        activeStyleIndex = ++activeStyleIndex % styles.length;

        Toast.makeText(this,
                STYLE_NAMES[activeStyleIndex] + " Style Shown", Toast.LENGTH_LONG).show();
        FritzOnDeviceModel onDeviceModel = styles[activeStyleIndex];
        predictor = FritzVision.StyleTransfer.getPredictor(onDeviceModel, options);
    }

    private void assignPredictor() {
        FritzOnDeviceModel[] styles = PaintingStyles.getAll();
        FritzOnDeviceModel onDeviceModel = styles[activeStyleIndex];
        predictor = FritzVision.StyleTransfer.getPredictor(onDeviceModel, options);
    }


    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image == null) {
            return;
        }

        if (!computing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        final FritzVisionImage fritzImage = FritzVisionImage.fromMediaImage(image, imageRotationFromCamera);
        image.close();


        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        styleResult = predictor.predict(fritzImage);
                        requestRender();
                        if (!snapshotButton.isEnabled())
                            hideSpinner();
                        computing.set(false);
                    }
                });
    }

    private void showSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotButton.setEnabled(false);
                spinner.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideSpinner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snapshotButton.setEnabled(true);
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private void showSnapshotLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlayView.setVisibility(View.GONE);
                snapshotLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPreviewLayout() {
        overlayView.setVisibility(View.VISIBLE);
        snapshotLayout.setVisibility(View.GONE);
    }

    private void endofsave() {
        String root = Environment.getExternalStorageDirectory().toString() + "/Styled photos/";
        File myDir = new File(root);
        myDir.mkdir();
        String fname = "Image-" + STYLE_NAMES[activeStyleIndex]+ SimpleDateFormat.getDateInstance().toString() + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            styleResult.getStyledImage().getBitmap().compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this,
                "Saved!", Toast.LENGTH_LONG).show();
        showPreviewLayout();
    }
    //TF
    public void myrun(Bitmap bitmap, Size size) {
        int[] intValues = new int[size.getWidth() * size.getHeight()];
        float[] floatValues = new float[size.getWidth() * size.getHeight() * 3];
        float[] outputValues = new float[size.getWidth() * size.getHeight() * 3];

        try {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, size.getWidth(), size.getHeight(), true);
            scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3 + 0] = ((val >> 16) & 0x00FF);
                floatValues[i * 3 + 1] = ((val >> 8) & 0x00FF);
                floatValues[i * 3 + 2] = (val & 0x00FF);
            }

            AssetManager assetManager = getAssets();
            mInferenceInterface = new TensorFlowInferenceInterface(assetManager, MODEL_FILE);

            // use self-trained Fast Style Transfer model
            mInferenceInterface.feed(INPUT_NODE, floatValues,  size.getHeight(), size.getWidth(), 3);
            mInferenceInterface.run(new String[] {OUTPUT_NODE}, false);
            mInferenceInterface.fetch(OUTPUT_NODE, outputValues);
            for (int i = 0; i < intValues.length; ++i) {
                intValues[i] = 0xFF000000
                        | (((int) outputValues[i * 3]) << 16)
                        | (((int) outputValues[i * 3 + 1]) << 8)
                        | ((int) outputValues[i * 3 + 2]);
            }

            Bitmap outputBitmap = scaledBitmap.copy( scaledBitmap.getConfig() , true);
            outputBitmap.setPixels(intValues, 0, outputBitmap.getWidth(), 0, 0, outputBitmap.getWidth(), outputBitmap.getHeight());

            outbitmap = Bitmap.createScaledBitmap(outputBitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}



