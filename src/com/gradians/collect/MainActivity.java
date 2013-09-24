package com.gradians.collect;

import java.io.File;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements ITaskCompletedListener, IConstants, OnItemSelectedListener {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initApp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                gradeables.adjust(quizPosn, questionPosn, triggerBtn == R.id.btnStartWS);
                setWidgets();                
                uploadImageHTTP();
            } else if (resultCode != RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        "Oops.. image capture failed. Please, try again",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == AUTH_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String error = null;
                try {
                    gradeables = new Gradeables(data.getStringExtra(TAG));
                } catch (Exception e) { 
                    error = e.getMessage() + "\n" + data.getStringExtra(TAG);
                }                
                if (error == null) {                    
                    SharedPreferences prefs = this.getSharedPreferences(TAG, 
                            Context.MODE_PRIVATE);
                    Editor edit = prefs.edit();
                    edit.putString(TOKEN_KEY, gradeables.getAuthToken());
                    edit.putString(NAME_KEY, gradeables.getName());
                    edit.putString(EMAIL_KEY, gradeables.getEmail());
                    edit.commit();
                    
                    setWidgets();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Oops, Scanbot stumbled :/ please try again",
                            Toast.LENGTH_SHORT).show();
                    //TODO: Send error email
                }                
            } else if (resultCode != Activity.RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),
                        "Oops, Scanbot stumbled :/ please try again",
                        Toast.LENGTH_SHORT).show();
            } else {
                this.finish();
            }
        }
    }
    
    @Override
    public void onTaskResult(int requestCode, int resultCode, String resultData) {
        if (requestCode == VERIFY_AUTH_TASK_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                String error = null;
                try {
                    gradeables = new Gradeables(resultData);
                } catch (Exception e) { 
                    error = e.getMessage() + "\n" + resultData;
                }
                if (error == null) {                    
                    setWidgets();
                    
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Oops, Scanbot stumbled :/ please try again",
                            Toast.LENGTH_SHORT).show();
                    //TODO: Send error email                    
                }
            } else {
                initiateAuthActivity();
            }
        } else if (requestCode == UPLOAD_IMAGE_TASK_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
                // Yay, nothing to do
            } else {
                //TODO: Send as email?
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      case R.id.action_sign_out:
          SharedPreferences prefs = getSharedPreferences(TAG, 
                  Context.MODE_PRIVATE);
          Editor edit = prefs.edit();
          edit.clear();
          edit.commit();
          initiateAuthActivity();
        break;
      default:
        break;
      }
      return super.onOptionsItemSelected(item);
    }    
    
    public void onToggleClicked(View view) {
        boolean on = ((ToggleButton)view).isChecked();
        if (on) {
            ((ToggleButton)view).setChecked(false);
            new AlertDialog.Builder(this).
                setTitle("Automatic Detection").
                setMessage("Warning: Only for worksheets with QR Codes!").
                setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int sumthin) {
                        autoMode(true);
                    }
                }).
                setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int sumthin) {
                        autoMode(false);
                    }
                }).
                show();
        } else {
            autoMode(false);
        }
    }
    
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos,
            long id) {
        Spinner spinner = (Spinner)parent;
        if (spinner.getId() == R.id.spnrQuiz) {            
            setSpinner(gradeables.getQuestionNames(pos), R.id.spnrQuestion);
        }        
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub        
    }    
    
    public void initiateCameraActivity(View view) {
        Log.v(TAG, "Button Id " + view.getId());
        File image = null;
        try {
            triggerBtn = view.getId();
            quizPosn = ((Spinner)findViewById(R.id.spnrQuiz)).
                    getSelectedItemPosition();
            questionPosn = ((Spinner)findViewById(R.id.spnrQuestion)).
                    getSelectedItemPosition();
            image = createImageFile(quizPosn, questionPosn, triggerBtn);
            Intent takePictureIntent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(image));
            startActivityForResult(takePictureIntent,
                    CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(context,
                    "Ooops... sorry, our bad :/",
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setWidgets() {
        Log.v(TAG, "Quizzes: " + gradeables.getQuizNames().length);
        setTitle("Scanbot - Hello, " + gradeables.getName() + "!");        
        String[] quizNames = gradeables.getQuizNames();
        setSpinner(quizNames, R.id.spnrQuiz);
        setSpinner(gradeables.getQuestionNames(0), R.id.spnrQuestion);
        ((Spinner)this.findViewById(R.id.spnrQuiz)).setOnItemSelectedListener(this);
    }

    private void initiateAuthActivity() {
        try {
            Intent checkAuthIntent = new Intent(context, 
                    com.gradians.collect.LoginActivity.class);
            checkAuthIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(checkAuthIntent, AUTH_ACTIVITY_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Oops... sorry, our bad :/", 
                    Toast.LENGTH_LONG).show();
        }
    }
    
    private void autoMode(boolean state) {
        //((ToggleButton)findViewById(R.id.tglAutoDetect)).setChecked(state);
        ((Spinner)findViewById(R.id.spnrQuestion)).setEnabled(!state);
        ((Spinner)findViewById(R.id.spnrQuiz)).setEnabled(!state);
    }      
    
    private void initApp() {
        appDir = new File(getExternalFilesDir(null), "Scandroid");
        context = getApplicationContext();
        SharedPreferences prefs = getSharedPreferences(TAG, 
                Context.MODE_PRIVATE);
        if (prefs.getString(TOKEN_KEY, null) == null) {
            initiateAuthActivity();
        } else {
            String email = prefs.getString(EMAIL_KEY, null);
            String token = prefs.getString(TOKEN_KEY, null);
            new VerificationTask(email, token, this).execute();
        }
    }    

    private void uploadImageHTTP() {
        File[] images = appDir.listFiles();
        new ImageUploadTask(this).execute(images);
    }
    
    private File createImageFile(int quizPosn, int questionPosn, int triggerBtn) throws Exception {
        if (!appDir.exists())
            appDir.mkdir();
        String prefix = triggerBtn == R.id.btnStartWS ? "QR" : "GR";
        String id = triggerBtn == R.id.btnStartWS ? 
                gradeables.getQRCode(quizPosn, questionPosn) : 
                gradeables.getGRId(quizPosn, questionPosn);
        String imageFileName = String.format("%s.%s.jpg", prefix, id);
        return new File(appDir, imageFileName);
    }
    
    private void setSpinner(String[] items, int resourceId) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_item, items);        
        ((Spinner)findViewById(resourceId)).setAdapter(adapter);
    }
    
    private int quizPosn, questionPosn, triggerBtn;
    private Context context;
    private Gradeables gradeables;
    private File appDir;
    
}
