// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelab.mlkit;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ImageView mImageView;
    private Button mTextButton;
    private Button mOpen;
    private Button mTakePhoto;
    private Bitmap mSelectedImage;
    private TextView mTextContent;
    // Max width (portrait mode)
    private Integer mImageMaxWidth;
    // Max height (portrait mode)
    private Integer mImageMaxHeight;
    /*????????????*/
    private boolean isPermissionPassed = false;
    private String toShow = "";
    private Integer RecogId = 0;

    /**
     * Number of results to show in the UI.
     */
    //private static final int RESULTS_TO_SHOW = 3;
    /**
     * Dimensions of inputs.
     */
    //private static final int DIM_IMG_SIZE_X = 224;
    //private static final int DIM_IMG_SIZE_Y = 224;

    private String mPath = "";
    //public static final int CAMERA_PERMISSION = 100;//?????????????????????
    public static final int REQUEST_HIGH_IMAGE = 101;//???????????????????????????
    public static final int REQUEST_LOAD_IMAGE = 102;

    /*
    private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float>
                                o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.image_view);
        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        DisplayMetrics dm = new DisplayMetrics();
        lp.width =  dm.widthPixels;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mImageView.setLayoutParams(lp);
        mImageView.setMaxWidth(dm.widthPixels);
        //mImageView.setMaxHeight((int)(dm.widthPixels*5));

        mTextButton = findViewById(R.id.button_text);
        mOpen = findViewById(R.id.open);
        mTakePhoto = findViewById(R.id.button_takePhoto);
        mTextContent = findViewById(R.id.textContent);
        /*??????????????????*/
        getPermission();
        /*??????????????????*/
        getPermissionsCamera();
        /*????????????????????????????????????*/
        mTakePhoto.setOnClickListener(v->{
            Intent highIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //?????????????????????URI???????????????????????????
            File imageFile = getImageFile();
            if (imageFile == null) return;
            //?????????????????????URI??????
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    "com.google.codelab.mlkit",//??????AndroidManifest.xml??????authorities ??????
                    imageFile
            );
            highIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
            startActivityForResult(highIntent,REQUEST_HIGH_IMAGE);//????????????
            mTextContent.setText("");
        });
        mOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_LOAD_IMAGE);
                mTextContent.setText("");
            }
        });

        mTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mImageView.getDrawable() == null){
                    showToast("Please select an image or take a photo");
                }
                else{
                    runTextRecognition();
                }
            }

        });



    }
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            isPermissionPassed = true;
        }
    }
    public void getPermissionsCamera(){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
    }
    private File getImageFile()  {
        String time = new SimpleDateFormat("yyMMdd").format(new Date());
        String fileName = time+"_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            //?????????????????????????????????
            File imageFile = File.createTempFile(fileName,".jpg",dir);
            //???????????????????????????????????????????????????????????????
            mPath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            return null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /*??????????????????*/
                isPermissionPassed = true;
            } else {
                /*?????????????????????*/
                if (ActivityCompat.shouldShowRequestPermissionRationale(this
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
                    getPermission();
                }
            }
        }
    }
    private void exportFile() {
        if (!isPermissionPassed) {
            Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        /*??????????????????*/
        DateFormat df = new SimpleDateFormat("MM_dd'T'HH_mm");
        String date = df.format(Calendar.getInstance().getTime());
        String fileName = "TextRecog" + date;
        /*??????????????????????????????*/
//      "/storage/emulated/0/Documents"
        String absoluteFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents";
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();
        try {
            /*????????????-> /storage/emulated/0/Documents/TextRecognitionOutput.txt*/
            File fileLocation =
                    new File(absoluteFilePath + "/" + fileName + ".txt");
            /*??????????????????*/
            FileOutputStream fos = new FileOutputStream(fileLocation);
            fos.write(toShow.getBytes());
            fos.close();
            showToast("Text save Success");
            RecogId = RecogId + 1;
        } catch (IOException e) {
            e.printStackTrace();
            showToast(e.getMessage());
        }
    }
    /**??????????????????*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*???????????????????????????????????????requestCode?????????????????????resultCode???-1??????????????????0????????????????????????*/
        Log.d(TAG, "onActivityResult: requestCode: "+requestCode+", resultCode "+resultCode);
        /*?????????????????????????????????*/
        if (requestCode == REQUEST_HIGH_IMAGE && resultCode == -1){
            ImageView imageHigh = findViewById(R.id.image_view);
            new Thread(()->{
                //???BitmapFactory????????????URI???????????????????????????????????????AtomicReference<Bitmap>???????????????????????????
                AtomicReference<Bitmap> getHighImage = new AtomicReference<>(BitmapFactory.decodeFile(mPath));
                Matrix matrix = new Matrix();
                matrix.setRotate(90f);//???90???
                getHighImage.set(Bitmap.createBitmap(getHighImage.get()
                        ,0,0
                        ,getHighImage.get().getWidth()
                        ,getHighImage.get().getHeight()
                        ,matrix,true));
                runOnUiThread(()->{
                    //???Glide????????????(?????????????????????????????????????????????LAG????????????????????????Thread)
                    Glide.with(this)
                            .load(getHighImage.get())
                            .centerCrop()
                            .into(imageHigh);
                });
            }).start();
        }
        else if(requestCode == REQUEST_LOAD_IMAGE){
            Uri uri = data.getData();
            Log.e("uri", uri.toString());
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                mPath = uri.getPath();
                /* ???Bitmap?????????ImageView */
                mImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                Log.e("Exception", e.getMessage(), e);
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
        else{
            Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
        }
    }
    private void runTextRecognition() {
        mSelectedImage = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        mTextButton.setEnabled(false);
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text texts) {
                                mTextButton.setEnabled(true);

                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                mTextButton.setEnabled(true);
                                e.printStackTrace();
                                showToast("No");

                            }
                        });
    }

    private void processTextRecognitionResult(Text texts) {
        toShow = "";
        for (Text.TextBlock block : texts.getTextBlocks()) {
            String blockText = block.getText();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                toShow = toShow + lineText + "\n";
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();

                }
            }
        }
        showToast(toShow);
        mTextContent.setText(toShow);
        exportFile();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Functions for loading images from app assets.

    // Returns max image width, always for portrait mode. Caller needs to swap width / height for
    // landscape mode.

}
