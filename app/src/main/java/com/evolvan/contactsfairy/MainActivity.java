package com.evolvan.contactsfairy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    String[] Parameter = { "SELECT","NAME", "PHONE", "EMAIL", "COMPANY" ,"JOB_TITLE", "POSTAL","URL","OTHERs"},str;
    private static final int openContactAndAddData=147,PICK_FROM_CAMERA = 123,PICK_FROM_GALLERY = 159, TIME_DELAY = 2000;
    private static long back_pressed;
    Intent intent;
    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "",getAppName,language = "eng";
    String[] sendValues={"NAME","PHONE","SECONDARY_PHONE","EMAIL","COMPANY","JOB_TITLE","POSTAL","IM_PROTOCOL","DATA"};
    LinearLayout Layouttime,layoutView;
    EditText e_mail;

    PackageManager packageManager;

    ImageView imageView,zoom_image,ButtonCancel;
    LayoutInflater layoutInflater;
    AlertDialog alertDialog;
    AlertDialog.Builder alertDialogBuilder;
    TextView allert_camera,alert_gallery;
    Spinner spin;
    ArrayAdapter aa;
    private Menu menu;
    List<String> al;
    List<String> add_match;
    int id=0;
    List<String> allEds = new ArrayList<String>();
    List<String>storeParameter=new ArrayList<>();
    private ExifInterface exifObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAppName=getString(R.string.app_name);
        imageView=(ImageView)findViewById(R.id.ImageView);

        //initialize Tesseract API
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
    public void open_Camera_or_Gallery(View openCameraGallery){
        layoutInflater = LayoutInflater.from(this);
        openCameraGallery = layoutInflater.inflate(R.layout.camera_gallery, null);
        alertDialogBuilder = new AlertDialog.Builder(this);
        // set prompts.xml to alertdialog notification
        alertDialogBuilder.setView(openCameraGallery);
        // create alert dialog
        alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        allert_camera = (TextView) openCameraGallery.findViewById(R.id.allert_camera);
        alert_gallery = (TextView) openCameraGallery.findViewById(R.id.alert_gallery);
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
                    processImageData();
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

                    if(imageView.getDrawable() != null){
                        try {
                            exifObject = new ExifInterface(imagePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        Bitmap imageRotate = rotateBitmap(bitmap,orientation);
                        imageView.setImageBitmap(imageRotate);
                    }else{
                        Toast.makeText(MainActivity.this, "Image photo is not yet set", Toast.LENGTH_LONG).show();
                    }

                    //imageView.setImageBitmap(bitmap);
                    processImageData();
                    break;
                }
                case openContactAndAddData: {
                    Toast.makeText(getApplicationContext(), "Thanks for using our "+getAppName, Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
    }

    //Image oriantation
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
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
    public void processImageData(){
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
            id=0;
            add_match=new ArrayList<>();
            Layouttime = (LinearLayout) findViewById(R.id.OCRTextContainer);
            allEds.clear();
            storeParameter.clear();
            Layouttime.removeAllViews();
            for(String s: al){
                //company name and domain name
                if ((s.contains("@")==false)&&(s.contains("."))){
                    String com=null;
                    //Website
                    Matcher Website =  Pattern.compile("(?:[a-z-]+\\.)+[a-z-]+").matcher(s);
                    while (Website.find()) {
                        showLayout(id,Website.group(),7);
                        com=Website.group();
                    }
                    if(!com.isEmpty()){
                        String[] split=com.split("\\.",2);
                        String tore=split[0];
                        showLayout(id,tore,4);
                    }
                }

                //phone number
                if(checkNumber(s)) {
                    showLayout(id, s, 2);
                }
                //email id
                else if(s.contains("@")){
                    showLayout(id,s,3);
                }
                // not varify
                else {
                    showLayout(id,s,0);
                }

                id++;
            }
            if(al.size()>0) {
                MenuItem item = menu.findItem(R.id.action_create);
                item.setVisible(true);
            }
        }
    }
    //identify phone number from string
    boolean checkNumber(String s){
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }
        if(count>=7)
            return true;

        return false;
    }

    public void showLayout(int id, String s,int i){

        layoutView = new LinearLayout(this);
        layoutView.setOrientation(LinearLayout.HORIZONTAL);
        layoutView.setId(id);

        spin = new Spinner(this);
        spin.setOnItemSelectedListener(this);
        aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Parameter);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(aa);
        spin.setSelection(i);
        spin.setId(id);
        storeParameter.add(spin.getItemAtPosition(id).toString());
        layoutView.addView(spin);

        e_mail = new EditText(this);
        e_mail.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        e_mail.setText(s);
        e_mail.setId(id);
        allEds.add(s);
        layoutView.addView(e_mail);

        Layouttime.addView(layoutView);


    }

    //now add all data in phone book
    public void Add_Data_In_Contacts_List(){

        if(al.size()>=0) {
            String[] strings=new String[al.size()];
            for(int file=0;file<al.size();file++) {

                    if (storeParameter.get(file).toString().equals(sendValues[file])) {
                        sendValues[file] = allEds.get(file).toString();
                    }

            }

            intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

            intent.putExtra(ContactsContract.Intents.Insert.NAME, sendValues[0])//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.PHONE, sendValues[1])//insert phone number of person
                    .putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE,sendValues[2])//insert alternate phone number of person
                    .putExtra(ContactsContract.Intents.Insert.EMAIL, sendValues[3])//insert email address of person
                    .putExtra(ContactsContract.Intents.Insert.COMPANY, sendValues[4])//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.JOB_TITLE, sendValues[5])//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.POSTAL, sendValues[6])//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.IM_PROTOCOL,sendValues[7])//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.DATA, sendValues[8]);//insert name of person

            packageManager = this.getPackageManager();
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent);
            } else {
                Something_went_wrong();
            }
        }else {
            Something_went_wrong();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
            storeParameter.add(spin.getItemAtPosition(position).toString());
        //Toast.makeText(getApplicationContext(),Parameter[position] ,Toast.LENGTH_SHORT).show();
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
