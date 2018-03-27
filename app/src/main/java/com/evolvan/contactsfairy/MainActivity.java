package com.evolvan.contactsfairy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    String[] Parameter = { "SELECT","NAME", "PHONE", "EMAIL", "COMPANY" ,"JOB_TITLE", "POSTAL","URL","OTHERs"},str,store_Intent_Values;
    private static final int PICK_FROM_CAMERA = 123,PICK_FROM_GALLERY = 159, TIME_DELAY = 2000;
    private static long back_pressed;
    Intent intent;
    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "",getAppName,language = "eng",com="",OCRresult="";
    String NAME="",PHONE="",SECONDARY_PHONE="",EMAIL="",COMPANY="",JOB_TITLE="",POSTAL="",IM_PROTOCOL="",DATA="";
    LinearLayout Layouttime,layoutView;
    EditText e_mail;
    ImageView zoom_image,ButtonCancel;
    CircleImageView imageView;
    LayoutInflater layoutInflater;
    AlertDialog alertDialog;
    AlertDialog.Builder alertDialogBuilder;
    Spinner spinner;
    ArrayAdapter spinnerArrayAdapter;
    private Menu menu;
    List<String> al;
    int id=0;
    List<String> StoreValues = new ArrayList<String>();
    List<String>storeParameter=new ArrayList<>();
    private ExifInterface exifObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAppName=getString(R.string.app_name);
        imageView=(CircleImageView)findViewById(R.id.ImageView);
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
        layoutInflater = LayoutInflater.from(this);
        zoomImage = layoutInflater.inflate(R.layout.zoom_view, null);
        alertDialogBuilder = new AlertDialog.Builder(this);
        // set prompts.xml to alertdialog notification
        alertDialogBuilder.setView(zoomImage);
        // create alert dialog
        alertDialog = alertDialogBuilder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        zoom_image = (ImageView) zoomImage.findViewById(R.id.zoom_image);
        if(bitmap!=null) {
            zoom_image.setImageBitmap(bitmap);
        }
        else {
            zoom_image.setImageDrawable(getResources().getDrawable(R.drawable.show_image));
        }
        ButtonCancel = (ImageView) zoomImage.findViewById(R.id.ButtonCancel);
        ButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    alertDialog.cancel();
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
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_FROM_GALLERY);
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
                    processImageData(bitmap);
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
                        this.bitmap=imageRotate;
                        processImageData(bitmap);
                    }else{
                        Something_went_wrong("Image photo is not yet set");
                    }
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
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    //extract image
    public void processImageData(Bitmap bitmap){
        this.bitmap=bitmap;
        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();
        str = OCRresult.split("\n");
        al = new ArrayList<String>();
        for(String s: str) {
            if((s.trim().length() > 0) || (!s.isEmpty())) {
                al.add(s);
            }
        }
        if(OCRresult.length()==0){
            Something_went_wrong("Sorry, we could not find any text in your image");
        }
        else {
            id=0;
            Layouttime = (LinearLayout) findViewById(R.id.OCRTextContainer);
            StoreValues.clear();
            storeParameter.clear();
            Layouttime.removeAllViews();
            for(String s: al){
                validation(s);
            }
            if(al.size()>0) {
                MenuItem item = menu.findItem(R.id.action_create);
                item.setVisible(true);
            }
        }
    }

    //data validation while parsing
    public void validation(String s){
        //company name and domain name
        if ((s.contains("@")==false)&&(s.contains("."))){
            //Website
            Matcher Website =  Pattern.compile("(?:[a-z-]+\\.)+[a-z-]+").matcher(s);
            while (Website.find()) {
                showLayout(Website.group(),7);
                com=Website.group();
            }
            if(!com.isEmpty()){
                String[] split=com.split("\\.",2);
                String tore=split[0];
                showLayout(tore,4);
            }
        }

        //phone number
        else if(checkNumber(s)) {
            String cc=s.replaceAll("[^+\\d]","");
            showLayout(cc, 2);
        }
        //email id
        else if(s.contains("@")){
            showLayout(s,3);
        }
        // not varify
        else {
            showLayout(s,0);
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
        if(count>=7) {
            return true;
        }
        return false;
    }

    public void showLayout(String s,int i){
        layoutView = new LinearLayout(this);
        layoutView.setOrientation(LinearLayout.HORIZONTAL);
        //Spinner field data binding
        spinner = new Spinner(this);
        spinner.setOnItemSelectedListener(this);
        spinnerArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Parameter);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(i);
        spinner.setId(id);
        storeParameter.add(spinner.getItemAtPosition(i).toString());
        layoutView.addView(spinner);
        //EditText field data binding
        e_mail = new EditText(this);
        e_mail.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        e_mail.setText(s);
        e_mail.setId(id);
        StoreValues.add(s);
        layoutView.addView(e_mail);
        Layouttime.addView(layoutView);
        //increament id to create number of view
        id++;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View views, int position,long id) {
        String checkdata=Parameter[position];
        if(checkdata.equals("COMPANY"))
        {
            String values= StoreValues.get(parent.getId()).replaceAll("[^A-Za-z0-9\\s]","");
            StoreValues.set(parent.getId(),values);
        }
        storeParameter.set(parent.getId(),Parameter[position]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {}

    //now add all data in phone book
    public void Add_Data_In_Contacts_List(){

        if(StoreValues.size()>=0) {
            int count=0;
            store_Intent_Values=new String[StoreValues.size()];
            for(int file=0;file<StoreValues.size();file++) {

                Log.d("getData", storeParameter.get(file) + "-" + StoreValues.get(file).toString());

                if(storeParameter.get(file).equals("NAME")){
                    NAME+=StoreValues.get(file).toString();
                }
                if(storeParameter.get(file).equals("PHONE")){
                    if(count==0) {
                        PHONE = StoreValues.get(file).toString();
                    }
                    if (count==1){
                        SECONDARY_PHONE = StoreValues.get(file).toString();
                    }
                    count++;
                }
                if(storeParameter.get(file).equals("EMAIL")){
                    EMAIL+=StoreValues.get(file).toString();
                }
                if(storeParameter.get(file).equals("COMPANY")){
                    COMPANY+=StoreValues.get(file);
                }
                if(storeParameter.get(file).equals("JOB_TITLE")){
                    JOB_TITLE+=StoreValues.get(file).toString();
                }
                if(storeParameter.get(file).equals("POSTAL")){
                    POSTAL+=StoreValues.get(file).toString();
                }
                if(storeParameter.get(file).equals("URL")){
                    IM_PROTOCOL+=StoreValues.get(file).toString();
                }
                if(storeParameter.get(file).equals("OTHERs")){
                    DATA+=StoreValues.get(file).toString();
                }
            }
            intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

            intent.putExtra(ContactsContract.Intents.Insert.NAME, NAME)//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.PHONE, PHONE)//insert phone number of person
                    .putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE,SECONDARY_PHONE)//insert alternate phone number of person
                    .putExtra(ContactsContract.Intents.Insert.EMAIL, EMAIL)//insert email address of person
                    .putExtra(ContactsContract.Intents.Insert.COMPANY, COMPANY)//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.JOB_TITLE, JOB_TITLE)//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.POSTAL, POSTAL)//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.IM_PROTOCOL,IM_PROTOCOL)//insert name of person
                    .putExtra(ContactsContract.Intents.Insert.DATA, DATA);//insert name of person

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Something_went_wrong("Unable to start service Intent");
            }
        }else {
            Something_went_wrong("Sorry, we could not find any text in your image");
        }
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
    public void Something_went_wrong(String s){
        String message="";
        if(s.isEmpty()) {
            message = "Something went wrong ! try again";
        }
        else {
            message=s;
        }
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
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
