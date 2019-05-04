package com.example.eliezer.selfadmin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {

    private Spinner dis_type;
    private EditText dis_name,dis_syptoms,dis_desc,dis_prevention,dis_cure;
    private ImageView imageView;
    private ImageButton imgDisease;
    private Button addDisease;
    DatabaseReference mDatabase;
    private StorageReference mStorage;
    Uri imageUri;
    Uri resultUri;
    private byte[] thumb_byte = new byte[0];
    private File thumb_filePath;
    private static final int PICK_IMAGE_REQUEST =1;
    private ProgressDialog progressDialog;
    String selectedType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       dis_type = findViewById(R.id.disease_type);
       ///dis_subtype = findViewById(R.id.disease_sub);
       dis_name = findViewById(R.id.disease_name);
       dis_syptoms = findViewById(R.id.disease_syptoms);
       dis_desc = findViewById(R.id.disease_description);
       dis_prevention = findViewById(R.id.disease_prevention);
       dis_cure = findViewById(R.id.disease_cure);

       imageView = findViewById(R.id.viewDisease);

       imgDisease = findViewById(R.id.disease_img);
       addDisease = findViewById(R.id.add_disease);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Disease");
        mStorage = FirebaseStorage.getInstance().getReference();

        String [] types = {"cancer","cutaneous","endocrine","eye","human","infectious","intestinal"};
        ArrayAdapter<String > adapter = new ArrayAdapter <String >(this,R.layout.support_simple_spinner_dropdown_item,types);
        dis_type.setAdapter(adapter);
        dis_type.setPrompt("Select type");

        dis_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView <?> parent, View view, int position, long id) {
                switch (position){
                    case 0 :
                        selectedType = "cancer";
                        break;
                    case 1 :
                        selectedType = "cutaneous";
                        break;
                    case 2 :
                        selectedType = "endocrine";
                        break;
                    case 3 :
                        selectedType = "eye";
                        break;
                    case 4 :
                        selectedType = "human";
                        break;
                    case 5 :
                        selectedType = "infectious";
                        break;
                    case 6 :
                        selectedType = "intestinal";
                        break;

                }
            }

            @Override
            public void onNothingSelected(AdapterView <?> parent) {
                Toast.makeText(MainActivity.this, "Please select type", Toast.LENGTH_SHORT).show();
            }
        });

        //button image to add disease image
        imgDisease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(MainActivity.this);*/
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), PICK_IMAGE_REQUEST);
            }
        });

        addDisease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String stType = dis_type.getText().toString();
                String stName = dis_name.getText().toString();
                String stsyptoms = dis_syptoms.getText().toString();
                String stDesc = dis_desc.getText().toString();
                String stPrevention = dis_prevention.getText().toString();
                String stCure = dis_cure.getText().toString();
                UploadImages(selectedType,stName,stsyptoms,stDesc,stPrevention,stCure);
                //Toast.makeText(MainActivity.this, selectedType, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageUri = data.getData();

            CropImage.activity(imageUri)
                    //.setAspectRatio(8, 8)
                    //.setMinCropWindowSize(800, 800)
                    .start(this);

            thumb_filePath = new File(imageUri.getPath());
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();

                thumb_filePath = new File(resultUri.getPath());

                //IF CROPPING IT'S OKAY, IT SHOULD DISPLAY THE IMAGE


                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                thumb_byte = baos.toByteArray();

            }
        }
    }


    public void UploadImages(final String disease_type, final String disease_name,
                             final String disease_syptoms,final String disease_desc, final String disease_prev,
                             final String disease_cure){
        final String push_id = mDatabase.push().getKey().toString();
        final StorageReference riversRef = mStorage.child("disease_images").child(push_id+".jpg");

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Upload Articles");
        progressDialog.setMessage("Wait until the uploading process is done");
        progressDialog.show();

            if (resultUri != null){

                UploadTask thumbTask = riversRef.putBytes(thumb_byte);
                thumbTask.addOnCompleteListener(new OnCompleteListener <UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task <UploadTask.TaskSnapshot> thumb_task) {
                        if (thumb_task.isSuccessful()){
                            final String thumb_url = thumb_task.getResult().getDownloadUrl().toString();

                            Map<String ,String> map = new HashMap <>();
                            map.put("cure",disease_cure);
                            map.put("description",disease_desc);
                            map.put("image",thumb_url);
                            map.put("prevention",disease_prev);
                            map.put("syptoms",disease_syptoms);
                            map.put("name",disease_name.toLowerCase());

                            mDatabase.child(disease_type).push().setValue(map).addOnCompleteListener(new OnCompleteListener <Void>() {
                                @Override
                                public void onComplete(@NonNull Task <Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(MainActivity.this, "Disease successfully added", Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();

                                        clearFields();
                                    }else{
                                        Toast.makeText(MainActivity.this, "Failed to add", Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
                                        Log.i("WORK", "insertion success");
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(MainActivity.this, "Thumbnail image not uploaded", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else{
                Toast.makeText(this, "Image uri is null", Toast.LENGTH_SHORT).show();
            }
    }

    public void clearFields(){
        dis_cure.setText("");
        dis_desc.setText("");
        dis_prevention.setText("");
        dis_syptoms.setText("");
        dis_name.setText("");
    }


}
