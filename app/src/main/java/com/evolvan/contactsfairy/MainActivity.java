package com.evolvan.contactsfairy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
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

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_SETTINGS;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private ProgressDialog progressDialog;
    String[] Parameter = { "SELECT","NAME", "PHONE", "EMAIL", "COMPANY" ,"JOB_TITLE", "ADDRESS","URL"},store_Intent_Values;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    Intent intent;
    Bitmap bitmap;
    private TessBaseAPI tessBaseAPI;
    String datapath = "",getAppName,language = "eng",OCRresult="";
    LinearLayout OCRTextContainer,layoutView;
    EditText editText;
    TextView notice;
    CircleImageView ImageView;
    Spinner spinner;
    ArrayAdapter spinnerArrayAdapter;
    private Menu menu;
    int id=0;
    List<String> StoreValues = new ArrayList<String>();
    List<String>storeParameter=new ArrayList<>();
    private Uri mCropImageUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);

        getAppName=getString(R.string.app_name);
        ImageView =(CircleImageView)findViewById(R.id.ImageView);
        notice=(TextView)findViewById(R.id.notice);
        OCRTextContainer = (LinearLayout) findViewById(R.id.OCRTextContainer);
        //initialize Tesseract API
        datapath = getFilesDir()+ "/tesseract/";
        tessBaseAPI = new TessBaseAPI();
        checkFile(new File(datapath + "tessdata/"));
        tessBaseAPI.init(datapath, language);
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

    // open camera to take picture for ocr
    public void Choose_Image_Camera(View view){
        CropImage.startPickImageActivity(this);
    }

    //check result after redirecting  image crop ot select from gallery
    @Override
    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // handle result of pick image chooser
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(this, data);

            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                mCropImageUri = imageUri;
                requestPermissions(new String[]{READ_EXTERNAL_STORAGE,WRITE_EXTERNAL_STORAGE,CAMERA,READ_CONTACTS,WRITE_CONTACTS,WRITE_SETTINGS}, 0);
            } else {
                // no permissions required or already grunted, can start crop image activity
                startCropImageActivity(imageUri);
            }
        }

        // handle result of CropImageActivity
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                ((ImageView) findViewById(R.id.ImageView)).setImageURI(result.getUri());
                new ImageExtracting().execute("");
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // required permissions granted, start crop image activity
            startCropImageActivity(mCropImageUri);
        }
        else {
            Toast.makeText(this, "Cancelling, required permissions are not granted", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Start crop image activity for the given image.
     */
    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).setMultiTouchEnabled(true).start(this);
    }

    //extract image
    public void processImageData(){
        BitmapDrawable drawable = (BitmapDrawable) ImageView.getDrawable();
        bitmap = drawable.getBitmap();
        String[] str=null;
        StoreValues.clear();
        storeParameter.clear();
        id=0;
        OCRTextContainer.removeAllViews();

        tessBaseAPI.setImage(bitmap);
        OCRresult = tessBaseAPI.getUTF8Text();
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
        if((s.contains("@") || s.contains("\\u00a9")) && s.contains(".")){
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
        Matcher NameMatcher =  Pattern.compile("\"\\'\",\"\\'\\'\"").matcher(s);
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
        if((s.contains("https")|| s.contains("www")) || (!s.contains("@") && !s.contains("&") && !s.contains(",") && s.contains(".") )){
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
        editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editText.setText(s);
        editText.setId(id);
        editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        StoreValues.add(s);
        layoutView.addView(editText);
        OCRTextContainer.addView(layoutView);
        //increament id to create number of view
        id++;
    }

    //validate when spinner item get change
    @Override
    public void onItemSelected(AdapterView<?> parent, View views, int position,long id) {
        storeParameter.set(parent.getId(),Parameter[position]);
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

    //on click imageview get image large/zoom
    public void zoom_Image(View zoomImage){
        if(checkBitMap()) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            zoomImage = layoutInflater.inflate(R.layout.zoom_view, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            // set prompts.xml to alertdialog notification
            alertDialogBuilder.setView(zoomImage);
            // create alert dialog
            final AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            ImageView zoom_image = (ImageView) zoomImage.findViewById(R.id.zoom_image);
            zoom_image.setImageBitmap(bitmap);
            ImageView ButtonCancel = (ImageView) zoomImage.findViewById(R.id.ButtonCancel);
            ButtonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.cancel();
                }
            });
            alertDialog.show();
        }
        else {
            CropImage.startPickImageActivity(this);
        }
    }

    //check bitmap have data or not
    public boolean checkBitMap(){
        if(bitmap!=null)
            return true;
        return false;
    }

    //extract image in background
    private class ImageExtracting extends AsyncTask<Object, Object, String[]> {
        String[] str=null;
        @Override
        protected void onPreExecute() {
            BitmapDrawable drawable = (BitmapDrawable) ImageView.getDrawable();
            bitmap = drawable.getBitmap();
            OCRTextContainer.removeAllViews();
            StoreValues.clear();
            storeParameter.clear();
            id=0;

            progressDialog.setMessage("Extracting...");
            progressDialog.show();
        }
        @Override
        protected String[] doInBackground(Object... params) {

            tessBaseAPI.setImage(bitmap);
            OCRresult = tessBaseAPI.getUTF8Text();
            str = OCRresult.split("\n");

            return str;
        }
        @Override
        protected void onPostExecute(String[] result) {
            if(result.length!=0){
                notice.setVisibility(View.GONE);
                for(String s: result) {
                    if((s.trim().length() > 0) || (!s.isEmpty())) {
                        validation(s);
                    }
                }
                progressDialog.dismiss();
                MenuItem item = menu.findItem(R.id.action_create);
                item.setVisible(true);
            }
        }
    }

}
