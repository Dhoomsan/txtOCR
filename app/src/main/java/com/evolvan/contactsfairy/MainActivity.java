package com.evolvan.contactsfairy;

import android.app.AlertDialog;
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
    String[] Parameter = { "SELECT","NAME", "PHONE", "EMAIL", "COMPANY" ,"JOB_TITLE", "ADDRESS","URL"},store_Intent_Values;
    private static final int PICK_FROM_CAMERA = 123,PICK_FROM_GALLERY = 159, TIME_DELAY = 2000;
    private static long back_pressed;
    Intent intent;
    Bitmap bitmap;
    private TessBaseAPI mTess;
    String datapath = "",getAppName,language = "eng",OCRresult="";
    LinearLayout Layouttime,layoutView;
    EditText e_mail;
    ImageView zoom_image,ButtonCancel;
    CircleImageView ImageView;
    LayoutInflater layoutInflater;
    AlertDialog alertDialog;
    AlertDialog.Builder alertDialogBuilder;
    Spinner spinner;
    ArrayAdapter spinnerArrayAdapter;
    private Menu menu;
    int id=0;
    List<String> StoreValues = new ArrayList<String>();
    List<String>storeParameter=new ArrayList<>();
    private ExifInterface exifObject;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAppName=getString(R.string.app_name);
        ImageView =(CircleImageView)findViewById(R.id.ImageView);
        Layouttime = (LinearLayout) findViewById(R.id.OCRTextContainer);
        //initialize Tesseract API
        datapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        mTess.init(datapath, language);
    }

    //restrict application to close on back press
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
        if(checkBitMap()) {
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

    //check bitmap have data or not
    public boolean checkBitMap(){
        if(bitmap!=null)
            return true;
        return false;
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

    //check intent select / capture image returning or not
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
                    if(checkBitMap()){
                        processImageData();
                    }
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
                    Bitmap bitmap11 = BitmapFactory.decodeFile(imagePath, options);
                    cursor.close();

                    if(ImageView.getDrawable() != null){
                        try {
                            exifObject = new ExifInterface(imagePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int orientation = exifObject.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        bitmap = rotateBitmap(bitmap11,orientation);
                        ImageView.setImageBitmap(bitmap);
                        if(checkBitMap()){
                            processImageData();
                        }
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
    public void processImageData(){
        String[] str=null;
        StoreValues.clear();
        storeParameter.clear();
        id=0;
        Layouttime.removeAllViews();

        mTess.setImage(bitmap);
        OCRresult = mTess.getUTF8Text();
        str = OCRresult.split("\n");
        if(OCRresult.length()!=0){
            for(String s: str) {
                if((s.trim().length() > 0) || (!s.isEmpty())) {
                    validation(s);
                }
            }
            MenuItem item = menu.findItem(R.id.action_create);
            item.setVisible(true);
        } else {
            Something_went_wrong("Sorry, we could not find any text in your image");
        }

    }

    //data validation while parsing
    public void validation(String s){
        //validate NAME from string 1
        if(!checkName(s).isEmpty()){
            showLayout(s,1);
        }
        //validate PHONE from string  2
        else if(!checkNumber(s).isEmpty()){
            showLayout(s,2);
        }
        //validate EMAIL from string 3
        else if (!checkEmail(s).isEmpty()){
            showLayout(s,3);
        }
        //validate COMPANY from string 4
        else if (!checkCompany(s).isEmpty()){
            showLayout(s,4);
        }
        //validate JOB_TITLE from string 5
        else if (!checkJobTitle(s).isEmpty()){
            showLayout(s,5);
        }
        //validate POSTAL from string 6
        else if (!checkPostal(s).isEmpty()){
            showLayout(s,6);
        }
        //validate URL from string 7
        else if (!checkUrl(s).isEmpty()){
            showLayout(s,7);
        }
        //validate not Validate from string  0
        else {
            showLayout(s, 0);
        }
    }

    //validate NAME from string 1
    public String checkName(String s){
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(s);
        boolean b = m.find();
        if (!b) {
            int count = 0;
            for (int i = 0, len = s.length(); i < len; i++) {
                if (Character.isUpperCase(s.codePointAt(0))) {
                    count++;
                }
            }
            if (count >=1) {
                return s;
            }
        }
        return "";
    }

    //validate PHONE from string  2
    public String checkNumber(String s){
        int count = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isDigit(s.charAt(i))) {
                count++;
            }
        }
        if(count>=7) {
            return s;
        }
        return "";
    }

    //validate EMAIL from string 3
    public String checkEmail(String s){
        if(s.contains("@") && s.contains(".")){
            return s;
        }
        return  "";
    }

    //validate COMPANY from string 4
    public String checkCompany(String s){
        if(!s.contains(".")&& !s.contains("'") && !s.contains(",")&& !s.matches("-?\\d+")) {
            String[] arr = s.split(" ");
            if (arr.length != 1) {
                for (int i = 0; i < s.length(); i++) {
                    if (Character.isUpperCase(s.charAt(i))) {
                        return s;
                    }
                }
            }
        }
        return  "";
    }

    //validate JOB_TITLE from string 5
    public String checkJobTitle(String s){
        Matcher NameMatcher =  Pattern.compile("\\\"([^\\\"\\n\\r]*)\\\"").matcher(s);
        while (NameMatcher.find()) {
            return NameMatcher.group();
        }
        return  "";
    }

    //validate POSTAL from string 6
    public String checkPostal(String s){
        if(s.contains(",")){
            return s;
        }
        return  "";
    }

    //validate URL from string 7
    public String checkUrl(String s){
        if((s.contains("https")||s.contains("www")) ||(!s.contains("@")&& s.contains("."))){
            return s;
        }
        return  "";
    }

    //show output in dynamic layout
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

    //validate when spinner item get change
    @Override
    public void onItemSelected(AdapterView<?> parent, View views, int position,long id) {
        storeParameter.set(parent.getId(),Parameter[position]);
        Log.d("printdata","item-"+storeParameter.get(parent.getId()));
    }

    //notify  when spinner item get change
    @Override
    public void onNothingSelected(AdapterView<?> arg0) {}

    //now add all data in phone book
    public void Add_Data_In_Contacts_List(){
        String NAME="",PHONE="",SECONDARY_PHONE="",EMAIL="",COMPANY="",JOB_TITLE="",ADDRESS="",IM_PROTOCOL="",DATA="";
        if(StoreValues.size()>=0) {
            int count=0;
            store_Intent_Values=new String[StoreValues.size()];
            for(int file=0;file<StoreValues.size();file++) {

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
                if(storeParameter.get(file).equals("ADDRESS")){
                    ADDRESS+=StoreValues.get(file).toString();
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
                    .putExtra(ContactsContract.Intents.Insert.POSTAL, ADDRESS)//insert name of person
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

    //create menu dynamically
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //on click menu item open phone book to save contacts
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
