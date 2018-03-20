package com.evolvan.contactsfairy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddContacts extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_GALLERY = 2;
    ImageView ImageView, ButtonCancel,zoom_image;
    Button Camera,Gallery;
    Intent intent;

    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "";

    Boolean resultok=false;

    LinearLayout Layouttime,layoutView;
    TextView mail;
    EditText e_mail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);
        //initialize
        ImageView=(ImageView)findViewById(R.id.ImageView);

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
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_FROM_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("getData","data= "+ String.valueOf(data));
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
                    Bundle extras = data.getExtras();
                    Bitmap bitmap1 = (Bitmap) extras.get("data");
                    Uri tempUri = getImageUri(getApplicationContext(), bitmap1);
                    String[] filePath = { MediaStore.Images.Media.DATA };
                    Cursor cursor = getContentResolver().query(tempUri, filePath, null, null, null);
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
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public void processImage(){
        String OCRresult = null;
        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();
        if(OCRresult.length()==0){
            Toast.makeText(getApplicationContext(), "Sorry, we could not find any text in your image", Toast.LENGTH_LONG).show();
        }
        else {
            resultok=true;
            addView(OCRresult);

        }
    }

    public void addView(String OCRresult){
      Layouttime = (LinearLayout) findViewById(R.id.OCRTextContainer);
         /* layoutView = new LinearLayout(this);
        layoutView.setOrientation(LinearLayout.HORIZONTAL);
        e_mail = new EditText(this);
        e_mail.setText(OCRresult);
        layoutView.addView(e_mail);
        Layouttime.addView(layoutView);*/
        //Name
       /* Matcher name =  Pattern.compile("").matcher(OCRresult);
        while (name.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            mail=new TextView(this);
            mail.setText("  Name : ");
            layoutView.addView(mail);
            e_mail = new EditText(this);
            e_mail.setText(name.group());
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }*/
        //number
        Matcher Number =  Pattern.compile("(\\d{3}[-\\.\\s]\\d{4}[-\\.\\s]\\d{3})|(\\d{10})|((?:\\/d{3}-){2}\\/d{4})|(\\(\\d{3}\\)\\d{3}-?\\d{4})|((?:\\(\\d{3}\\)|\\d{3}[-]*)\\d{3}[-]*\\d{4})|(^[+]?[01]?[- .]?(\\([2-9]\\d{2}\\)|[2-9]\\d{2})[- .]?\\d{3}[- .]?\\d{4}$)").matcher(OCRresult);
        while (Number.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            TextView mail=new TextView(this);
            mail.setText("  Number : ");
            layoutView.addView(mail);
            TextView e_mail = new EditText(this);
            e_mail.setText(Number.group());
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }
        //email
        Matcher e_Mail = Pattern.compile("[a-zA-Z0-9'_.+-]+@[a-zA-Z0-9-']+\\.[a-zA-Z0-9-.]+").matcher(OCRresult);
       // Matcher e_Mail = Pattern.compile("[a-zA-Z0-9._-]+@[a-z]+\\\\.+[a-z]+").matcher(OCRresult);

        while (e_Mail.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            TextView mail=new TextView(this);
            mail.setText("  e_Mail : ");
            layoutView.addView(mail);
            TextView e_mail = new EditText(this);
            e_mail.setText(e_Mail.group());
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }
        //Organization
        Matcher Organization =  Pattern.compile("(?:[\\w-]+\\.)+[\\w-]+").matcher(OCRresult);
        while (Organization.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            mail=new TextView(this);
            mail.setText("  Organization : ");
            layoutView.addView(mail);
            e_mail = new EditText(this);
            String[] split=Organization.group().split("\\.",2);
            String tore=split[0];
            e_mail.setText(tore);
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }

        //Website
        Matcher Website =  Pattern.compile("(?:[\\w-]+\\.)+[\\w-]+").matcher(OCRresult);
        while (Website.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);

            mail=new TextView(this);
            mail.setText("  Website : ");
            layoutView.addView(mail);

            e_mail = new EditText(this);
            e_mail.setText(Website.group());
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }
        /*//Address
        Matcher Address =  Pattern.compile("").matcher(OCRresult);
        while (Address.find()) {
            layoutView = new LinearLayout(this);
            layoutView.setOrientation(LinearLayout.HORIZONTAL);
            mail=new TextView(this);
            mail.setText("  Address : ");
            layoutView.addView(mail);
            e_mail = new EditText(this);
            e_mail.setText(Address.group());
            layoutView.addView(e_mail);
            Layouttime.addView(layoutView);
        }*/

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
