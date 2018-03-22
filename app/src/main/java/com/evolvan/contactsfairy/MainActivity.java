package com.evolvan.contactsfairy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    String[] Parameter = { "Name", "Number", "EMail", "Work", "Company" , "Address" , "Link", "Other" };
    private static final int openContactAndAddData=147;
    private static final int PICK_FROM_CAMERA = 123;
    private static final int PICK_FROM_GALLERY = 159;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    Intent intent;
    PackageManager packageManager;
    String[] sendValues ={"Name","Number","EMail","Company"};

    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "",getAppName;

    LinearLayout Layouttime,layoutView;
    EditText e_mail;

    ImageView imageView,zoom_image,ButtonCancel;
    LayoutInflater layoutInflater;
    AlertDialog alertDialog;
    AlertDialog.Builder alertDialogBuilder;
    TextView allert_camera,alert_gallery;
    Spinner spin;
    ArrayAdapter aa;
    String[] str,storeParameter,storeValues;
    private Menu menu;
    List<String> al;
    List<EditText> allEds = new ArrayList<EditText>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAppName=getString(R.string.app_name);
        imageView=(ImageView)findViewById(R.id.ImageView);

        //initialize Tesseract API
        String language = "eng";
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);

    }

    @Override
    public void onBackPressed() {
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Press once again to exit!", Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }

    //on click image zoomm it
    public void zoom_Image(View zoomImage){
        if (bitmap != null) {
            layoutInflater = LayoutInflater.from(this);
            zoomImage = layoutInflater.inflate(R.layout.zoom_view, null);
            alertDialogBuilder = new AlertDialog.Builder(this);
            // set prompts.xml to alertdialog notification
            alertDialogBuilder.setView(zoomImage);
            // create alert dialog
            alertDialog = alertDialogBuilder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            zoom_image = (ImageView) zoomImage.findViewById(R.id.zoom_image);
            zoom_image.setImageBitmap(bitmap);
            ButtonCancel = (ImageView) zoomImage.findViewById(R.id.ButtonCancel);
            ButtonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.cancel();
                }
            });
            alertDialog.show();
        }
        else {
            open_Camera_or_Gallery(zoomImage);
        }

    }

    //if image not selected choose oprations
    public void open_Camera_or_Gallery(View zoomImage){
        layoutInflater = LayoutInflater.from(this);
        zoomImage = layoutInflater.inflate(R.layout.camera_gallery, null);
        alertDialogBuilder = new AlertDialog.Builder(this);
        // set prompts.xml to alertdialog notification
        alertDialogBuilder.setView(zoomImage);
        // create alert dialog
        alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        allert_camera = (TextView) zoomImage.findViewById(R.id.allert_camera);
        alert_gallery = (TextView) zoomImage.findViewById(R.id.alert_gallery);
        allert_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Open_Camera(v);
                alertDialog.dismiss();
            }
        });
        alert_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Open_Gallery(v);
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    // open camera to take picture for ocr
    public void Open_Camera(View OpenCamera){
        intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_FROM_CAMERA);
        }
    }

    //open gallery to select image to extract text
    public void Open_Gallery(View OpenGallery){
        intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_FROM_GALLERY);
    }

    //now add all data in phone book
    public void Add_Data_In_Contacts_List(){
        if(al.size()>=0) {
            for (int i=0;i<al.size();i++) {
                storeValues[i]= allEds.get(i).getText().toString();
                if(i<4) {
                    sendValues[i] = storeValues[i];
                }
                Log.d("printStoreValues", storeParameter[i] + "-" + storeValues[i]);
            }
        }

        intent= new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        intent  .putExtra(ContactsContract.Intents.Insert.NAME, sendValues[0])
                .putExtra(ContactsContract.Intents.Insert.PHONE, sendValues[1])
                .putExtra(ContactsContract.Intents.Insert.EMAIL, sendValues[2])
                .putExtra(ContactsContract.Intents.Insert.COMPANY, sendValues[3]);
        packageManager = this.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, openContactAndAddData);
        } else {
            Something_went_wrong();
        }
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

                    imageView.setImageBitmap(bitmap);
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

                    imageView.setImageBitmap(bitmap);
                    processImage();
                    break;
                }
                case openContactAndAddData: {
                    Toast.makeText(getApplicationContext(), "Thanks for using our "+getAppName, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    //get intent details
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    //extract image
    public void processImage(){
        String OCRresult = null;
        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();
        str = OCRresult.split("\n");

        al = new ArrayList<String>();
        for(String s: str) {
            if(s.trim().length() > 0) {
                al.add(s);
            }
        }

        if(OCRresult.length()==0){
            Toast.makeText(getApplicationContext(), "Sorry, we could not find any text in your image", Toast.LENGTH_LONG).show();
        }
        else {
            int id=0;
            Layouttime = (LinearLayout) findViewById(R.id.OCRTextContainer);
            Layouttime.removeAllViews();
            storeParameter=new String[al.size()];
            storeValues=new String[al.size()];
            for(String s: al){
                layoutView = new LinearLayout(this);
                layoutView.setOrientation(LinearLayout.HORIZONTAL);

                spin = new Spinner(this);
                spin.setOnItemSelectedListener(this);
                aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Parameter);
                aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spin.setAdapter(aa);
                spin.setId(id);
                layoutView.addView(spin);

                e_mail = new EditText(this);
                e_mail.setText(s);
                allEds.add(e_mail);
                e_mail.setId(id);

                layoutView.addView(e_mail);
                Layouttime.addView(layoutView);

                storeParameter[id]=spin.getSelectedItem().toString();

                id++;
            }
            if(al.size()>0) {
                MenuItem item = menu.findItem(R.id.action_create);
                item.setVisible(true);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        storeParameter[position]=spin.getItemAtPosition(position).toString();
        //Toast.makeText(getApplicationContext(),Parameter[position] ,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    //authontication
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

    //copy file
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

    //error
    public void Something_went_wrong(){
        Toast.makeText(this,"Something went wrong ! try again",Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_create) {
            Add_Data_In_Contacts_List();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
