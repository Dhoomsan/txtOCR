package com.evolvan.contactsfairy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.hdodenhof.circleimageview.CircleImageView;
//requered permissions
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.MANAGE_DOCUMENTS;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_SETTINGS;

public class MainActivity extends AppCompatActivity{

    String[] Parameter = { "SELECT","NAME", "PHONE", "EMAIL", "COMPANY" ,"JOB_TITLE", "ADDRESS","URL"}; String datapath = "",getAppName,language = "eng";
    private static final int TIME_DELAY = 2000; private static final int PERMISSION_REQUEST_CODE = 200; private static long back_pressed;  private TessBaseAPI tessBaseAPI;
    Intent intent;  Bitmap bitmap; LinearLayout OCRTextContainer; TextView notice; CircleImageView ImageView; private Menu menu; private Uri mCropImageUri;  private ProgressDialog progressDialog;
    ImageExtracting imageExtracting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //show  actionbar icon
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        progressDialog = new ProgressDialog(this);

        getAppName=getString(R.string.app_name);
        ImageView =(CircleImageView)findViewById(R.id.ImageView);
        notice=(TextView)findViewById(R.id.notice);
        OCRTextContainer = (LinearLayout) findViewById(R.id.OCRTextContainer);

        imageExtracting=new ImageExtracting();

    }

    //restrict application to close on back press
    @Override
    public void onBackPressed() {
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Something_went_wrong("Press once again to exit!");
        }
        back_pressed = System.currentTimeMillis();
    }

    // open camera to take picture for ocr
    public void Choose_Image_Camera(View view){
        if(checkPermission()) {
            CropImage.startPickImageActivity(this);
        }
        else {
            requestPermission();
        }
    }

    //request runtime permission
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, CAMERA}, PERMISSION_REQUEST_CODE);
    }

    //check prermission
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
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
            Log.d("cropData","crop"+result);
            if (resultCode == RESULT_OK) {
                ((ImageView) findViewById(R.id.ImageView)).setImageURI(result.getUri());

                BitmapDrawable drawable = (BitmapDrawable) ImageView.getDrawable();
                bitmap = drawable.getBitmap();
                if(checkBitMap()) {
                    new ImageExtracting().execute("");
                }
                else {
                    Something_went_wrong("");
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Something_went_wrong("\"Cropping failed: " + result.getError());
            }
        }
    }

    //requestPermission call back result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (mCropImageUri != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // required permissions granted, start crop image activity
            startCropImageActivity(mCropImageUri);
        }
        else if(requestCode==PERMISSION_REQUEST_CODE && grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED){
            CropImage.startPickImageActivity(this);
        }
        else {
            permissionAlert();
        }
    }

    //show permission alert
    public void permissionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage("Need permission to read text.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Something_went_wrong("Cancelling, required permissions are not granted");
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.setIcon(R.mipmap.ic_launcher);// dialog  Icon
        alert.setTitle("Permission necessary"); // dialog  Title
        alert.show();
    }

    /**
     * Start crop image activity for the given image.
     */
    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).setMultiTouchEnabled(true).start(this);
    }

    //data validation while parsing
    public void validation(String strValidate){
        String str="",strAdd="";

        for (String name:strValidate.split("\n")){
            String FINAL_CHAR_REGEX = "[!#$%^&*()[\\\\]|;'/{}\\\\\\\\:\\\"<>?]";
            int specialCharCount = name.split(FINAL_CHAR_REGEX, -1).length - 1;
            if(specialCharCount==0) {
                Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(name);
                boolean b = m.find();
                if (!b) {
                    int count = 0;
                    for (int i = 0, len = name.length(); i < len; i++) {
                        if (Character.isUpperCase(name.codePointAt(0))) {
                            count++;
                        }
                    }
                    if (count >= 1) {
                        strValidate = strValidate.replaceAll("\\w*" + name + "\\w*", "").trim();
                        createDynamicLayout(name, 1);
                        break;
                    }
                }
            }
        }

        str=strValidate.replaceAll("[^+A-Za-z0-9\\s]+","");
        for(String number : str.split("\n")) {
            Pattern patternNumber = Pattern.compile("[\\d]{10,11}");
            Matcher matcherNumber = patternNumber.matcher(number);
            while (matcherNumber.find()) {
                strValidate = strValidate.replaceAll("\\w*" + matcherNumber.group() + "\\w*", "").trim();
                createDynamicLayout(matcherNumber.group(), 2);
            }
        }

        for(String email : strValidate.split("\n")){
            Matcher matcherEmail = Patterns.EMAIL_ADDRESS.matcher(email);
            while (matcherEmail.find()) {
                strValidate=strValidate.replaceAll("\\w*"+matcherEmail.group()+"\\w*", "").trim();
                createDynamicLayout(matcherEmail.group(), 3);
            }
        }

        for(String Url : strValidate.split(" ")){
            if(!Url.contains("@")) {
                Matcher matcherUrl = Patterns.WEB_URL.matcher(Url);
                while (matcherUrl.find()) {
                    strValidate=strValidate.replaceAll("\\w*"+matcherUrl.group()+"\\w*", "").trim();
                    if(matcherUrl.group().equals(matcherUrl.group().toLowerCase())) {
                        createDynamicLayout(matcherUrl.group(), 7);
                    }
                }
            }
        }

        for(String company:strValidate.split("\n")){
            String FINAL_CHAR_REGEX = "[!#$%^&*()[\\\\]|;'/{}\\\\\\\\:\\\"<>?]";
            int specialCharCount = company.split(FINAL_CHAR_REGEX, -1).length - 1;
            if(specialCharCount==0){
                if (company.toUpperCase().equals(company) && company.trim().length()>4) {
                    strValidate=strValidate.replaceAll("\\w*"+company+"\\w*", "").trim();
                    createDynamicLayout(company, 4);
                }
            }

        }

        strAdd=strValidate.replaceAll("\\w*@\\w*", "").trim();
        for(String Address : strAdd.split("\n")){
            if(!Address.contains("@") && Address.length()>10) {
                String regexAdd= "[a-zA-Z\\d\\s\\-\\,\\#\\.\\+]+";
                Matcher matcherAdd = Pattern.compile(regexAdd).matcher(Address);
                if (matcherAdd.find()) {
                    createDynamicLayout(matcherAdd.group(), 6);
                }
            }
        }

    }

    //add manual layoyt
    public void Add_Layout(View Add_Layout){
        createDynamicLayout("Enter...", 0);
    }

   //show output in dynamic layout
   public void createDynamicLayout(String s, int i){
       if(s.length()>2 && !s.isEmpty()) {
           MenuVisible();

           LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           final View addView = layoutInflater.inflate(R.layout.display_layout, null);
           Spinner spinner = (Spinner) addView.findViewById(R.id.spinner_id);
           EditText edittext = (EditText) addView.findViewById(R.id.edittext);

           ArrayAdapter spinnerArrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_textview_to_spinner, Parameter);
           spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
           spinner.setAdapter(spinnerArrayAdapter);
           spinner.setSelection(i);

           edittext.setText(s);

           ImageView remove = (ImageView) addView.findViewById(R.id.remove);
           remove.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   showAlert(addView);
               }
           });

           OCRTextContainer.addView(addView, -1);
       }
       else  {
           if(OCRTextContainer.getChildCount()==0) {
               notice.setVisibility(View.VISIBLE);
               notice.setText("Sorry, we could not find any text in your image.");
           }
       }

    }

    //show aler before delete data
    public void showAlert(final View addView){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage("Do you really want to Delete it ?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((LinearLayout)addView.getParent()).removeView(addView);

                        MenuGone();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.setIcon(R.mipmap.ic_launcher);// dialog  Icon
        alert.setTitle("Confirmation"); // dialog  Title
        alert.show();
    }

    //now add all data in phone book
    public void Add_Data_In_Contacts_List(){
        //Collections.reverse(StoreValues);
        int childCount = OCRTextContainer.getChildCount();
        String NAME="",PHONE="",SECONDARY_PHONE="",EMAIL="",COMPANY="",JOB_TITLE="",ADDRESS="", WEBADDRESS ="",DATA="";
        int count=0;
        if(childCount>0){

            for(int c=0; c<childCount; c++){

                View childView = OCRTextContainer.getChildAt(c);

                EditText childTextView = (EditText) (childView.findViewById(R.id.edittext));
                Spinner spinner=(Spinner)(childView.findViewById(R.id.spinner_id));

                String setValues = String.valueOf(childTextView.getText());
                String setParameter = (String)(spinner.getSelectedItem());

                if(setParameter.equals("NAME")){
                    NAME+=" " + setValues;
                }
                else if(setParameter.equals("PHONE")){
                    if(count==0) {
                        PHONE = setValues;
                    }
                    if (count==1){
                        SECONDARY_PHONE = setValues;
                    }
                    count++;
                }
                else if(setParameter.equals("EMAIL")){
                    EMAIL+=" " + setValues;
                }
                else if(setParameter.equals("COMPANY")){
                    COMPANY+=" " + setValues;
                }
                else if(setParameter.equals("JOB_TITLE")){
                    JOB_TITLE+=" " + setValues;
                }
                else if(setParameter.equals("ADDRESS")){
                    ADDRESS+=" " + setValues;
                }
                else if(setParameter.equals("URL")){
                    WEBADDRESS +=" " + setValues;
                }
                else {
                    DATA+=" " + setValues;
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
              .putExtra(ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE, WEBADDRESS)//insert name of person
              .putExtra(ContactsContract.Intents.Insert.DATA, DATA);//insert name of person
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }else {
                Something_went_wrong("Unable to start Intent service");
            }
        }else {
            Something_went_wrong("Sorry, unable to process !");
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

        switch (item.getItemId()){
            case R.id.action_create:{
                Add_Data_In_Contacts_List();
                break;
            }
            case R.id.action_save:{
                if(checkPermission()) {
                    Save_as_txt();
                }
                else {
                    requestPermission();
                }
                break;
            }
            /*case R.id.action_Refresh:{
                if(checkBitMap()){
                    new ImageExtracting().execute("");
                }
                else {
                    Something_went_wrong("");
                }
                break;
            }*/
            default:
                Something_went_wrong("");
        }
        return super.onOptionsItemSelected(item);
    }

    //save extract text.txt
    public void Save_as_txt(){
        String str="";
        String DNAME = getString(R.string.app_name);
        final File extStore =new File( Environment.getExternalStorageDirectory(), DNAME);
        // ==> /storage/emulated/0/note.txt
        final EditText fileName = new EditText(this);
        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setView(fileName);
        ad.setMessage("Write file name");
        int childCount = OCRTextContainer.getChildCount();
        if(childCount>0) {

            for (int c = 0; c < childCount; c++) {

                View childView = OCRTextContainer.getChildAt(c);

                EditText childTextView = (EditText) (childView.findViewById(R.id.edittext));
                Spinner spinner=(Spinner)(childView.findViewById(R.id.spinner_id));
                String setValues = childTextView.getText().toString().trim();
                String setParameter = (String)(spinner.getSelectedItem());

                str +="\n" +setParameter+" -: "+setValues;
            }
        }
        if (TextUtils.isEmpty(str)) {
            Something_went_wrong("No data to Save");
        } else {
            final String finalStr = str;
            ad.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!extStore.exists()) {
                        extStore.mkdirs();
                    }
                    String path = extStore.getAbsolutePath() + "/" + fileName.getText().toString() + ".txt";
                    try {
                        File myFile = new File(path);
                        myFile.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(myFile);
                        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                        myOutWriter.append(finalStr);
                        myOutWriter.close();
                        fOut.close();
                        Something_went_wrong("Saved");
                    } catch (Exception e) {
                        createNetErrorDialog();
                    }
                }
            });

            ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            ad.show();
        }

    }

    //storage error
    protected void createNetErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Go to"+"\n"+"Settings->Tap'Apps'->Choose App"+"\n"+"->App Permissions"+"\n"+"Enable it")
                .setTitle("storage or File not Exist!")
                .setCancelable(false)
                .setPositiveButton("Open",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        }
                )
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
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
            Choose_Image_Camera(zoomImage);
        }
    }

    //check bitmap have data or not
    public boolean checkBitMap(){
        if(bitmap!=null)
            return true;
        return false;
    }

    //extract image in background
    private class ImageExtracting extends AsyncTask<Object, Object, String> {
        String OCRresult=null;
        Boolean cancel=true;

        @Override
        protected void onPreExecute() {
            //initialize Tesseract API
            datapath = getFilesDir()+ "/tesseract/";
            tessBaseAPI = new TessBaseAPI();
            tessBaseAPI.setDebug(true);
            checkFile(new File(datapath + "tessdata/"));
            tessBaseAPI.init(datapath, language);
            //tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
            tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN);//ok

            OCRTextContainer.removeAllViews();
            progressDialog.setMessage("Processing, please wait.\n" + "An image with a lot of text can require some time.");
            progressDialog.setCancelable(false);
            progressDialog.setButton("Cancel", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    notice.setVisibility(View.VISIBLE);
                    notice.setText("Sorry, we could not find any text in your image.");
                    imageExtracting.cancel(true);
                    cancel=false;
                    progressDialog.dismiss();
                }
            });
            progressDialog.show();
        }
        @Override
        protected String doInBackground(Object... params) {
            tessBaseAPI.setImage(bitmap);
            OCRresult = tessBaseAPI.getUTF8Text();
            Log.d("getData",OCRresult);
            tessBaseAPI.end();

            return OCRresult;
        }
        @Override
        protected void onPostExecute(String result) {
            if(result.length()!=0 && cancel){
                notice.setText("");
                notice.setVisibility(View.GONE);
                validation(result);
                if(progressDialog!=null) {
                    progressDialog.dismiss();
                    imageExtracting.cancel(true);
                    MenuVisible();
                }
            }
        }
    }

    public void MenuVisible(){
        Log.d("getMenu","open");

        if(!menu.hasVisibleItems()) {
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(true);
            /*if(checkBitMap()) {
                menu.getItem(2).setVisible(true);
            }*/
            noticeGone();
        }
    }

    public void MenuGone(){
        Log.d("getMenu","close");
        if(menu.hasVisibleItems() && OCRTextContainer.getChildCount()==0) {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
            /*menu.getItem(2).setVisible(false);
*/            noticeVisible();
        }
    }

    public void noticeVisible(){
        if(notice.getVisibility() == View.GONE)
            notice.setVisibility(View.VISIBLE);
    }

    public void noticeGone(){
        if(notice.getVisibility() == View.VISIBLE)
            notice.setVisibility(View.GONE);
    }
}
