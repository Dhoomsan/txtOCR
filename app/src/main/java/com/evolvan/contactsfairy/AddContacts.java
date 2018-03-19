package com.evolvan.contactsfairy;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddContacts extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_GALLERY = 2;
    ImageView ImageView, ButtonCancel,zoom_image;
    TextView TextView,ulr_view,OCRTextView;
    Button Camera,Gallery;
    Intent intent;

    File file;
    Uri fileUri;
    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "";

    Boolean resultok=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);
        //initialize
        ImageView=(ImageView)findViewById(R.id.ImageView);
        TextView=(TextView)findViewById(R.id.TextView);
        OCRTextView = (TextView) findViewById(R.id.TextView);
        ulr_view=(TextView)findViewById(R.id.ulr_view);
        Camera=(Button)findViewById(R.id.Camera);
        Gallery=(Button)findViewById(R.id.Gallery);

        //click listener
        Camera.setOnClickListener(this);
        Gallery.setOnClickListener(this);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);
    }

    public void ClickImages(View view){
        if(resultok){
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.zoom_view, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            // set prompts.xml to alertdialog notification
            alertDialogBuilder.setView(promptsView);
            // create alert dialog
            final AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            zoom_image = (ImageView) promptsView.findViewById(R.id.zoom_image);
            zoom_image.setImageBitmap(bitmap);
            ButtonCancel = (ImageView) promptsView.findViewById(R.id.ButtonCancel);
            ButtonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.cancel();
                }
            });
            alertDialog.show();
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Camera: {
                OpenCamera();
                break;
            }
            case R.id.Gallery: {
                OpenGallery();
                break;
            }
        }
    }

    public void OpenGallery(){
        intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_FROM_GALLERY);
    }

    public void OpenCamera(){
        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(this.getExternalCacheDir(), String.valueOf(System.currentTimeMillis()) + ".jpg");
        fileUri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, PICK_FROM_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK && null != data) {
            switch (requestCode) {
                case PICK_FROM_GALLERY: {
                    Uri pickedImage = data.getData();
                    String[] filePath = { MediaStore.Images.Media.DATA };
                    Cursor cursor = getContentResolver().query(pickedImage, filePath, null, null, null);
                    cursor.moveToFirst();
                    String imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    bitmap = BitmapFactory.decodeFile(imagePath, options);
                    cursor.close();

                    ImageView.setImageBitmap(bitmap);
                    processImage();
                    break;
                }
                case PICK_FROM_CAMERA: {
                    String[] filePath = { MediaStore.Images.Media.DATA };
                    Cursor cursor = this.managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePath, null, null, null);
                    cursor.moveToFirst();

                    String imagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    bitmap = BitmapFactory.decodeFile(imagePath, options);
                    cursor.close();

                    ImageView.setImageBitmap(bitmap);
                    processImage();
                    break;
                }
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Operation could not be completed ! \n try again", Toast.LENGTH_LONG).show();
        }
    }

    public void processImage(){
        String OCRresult = null;
        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();
        OCRTextView.setText(OCRresult);
        if(OCRTextView.getText().length()==0){
            Toast.makeText(getApplicationContext(), "Sorry, we could not find any text in your image", Toast.LENGTH_LONG).show();
        }
        else {
            List<String> extractedUrls = extractUrls(OCRresult);
            for (String url : extractedUrls)
            {
                ulr_view.setText(url);
            }
            resultok=true;
        }
    }
    private static List<String> extractUrls(String text)
    {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find())
        {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }

        return containedUrls;
    }

    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
