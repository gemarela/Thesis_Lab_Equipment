package com.gmarelas.uthlabsequipment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/*Main class which is responsible for showing the tapped item info, the options(show database, add item, help, about)
* and showing proper messages to the user for enabling wireless and nfc*/
public class MainActivity extends Activity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NFCUTH";
    public static final int ACTIVITY_CREATE = 1;
    public static final int GENERAL_SETTINGS = 0;
    public static final int NFC_SETTINGS = 2;
    public static final int SHOW_DB = 3;
    public static final int ADD_ITEM = 4;
    private static boolean logged_in = false;
    private NfcAdapter mNfcAdapter;
    private TextView tag, type, model, sn, st, supplier, specs, date, producer, cost, warranty, receipt, location;
    private ProgressDialog progressDialog;
    private Dialog login;
    private String added_by;
    private String tag_id;
    private int id_action;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wait_tap);

        boolean nfc = false;    //flag used for nfc support
        progressDialog = new ProgressDialog(MainActivity.this);

        TextView wait_tap = (TextView) findViewById(R.id.tv_wait_tap);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            wait_tap.setText(R.string.no_tap);      //if nfc is supported show the proper message
        } else {
            wait_tap.setText(R.string.wait_tap);    //if nfc is not supported show the proper message
            nfc = true;
        }

        if(nfc){        //if nfc is supported then check if any internet connection and nfc are enabled
            if (!mNfcAdapter.isEnabled() && !isOnline()) {
                settingsCheck(getResources().getString(R.string.both), GENERAL_SETTINGS);
            } else if(!isOnline()){
                settingsCheck(getResources().getString(R.string.no_internet), GENERAL_SETTINGS);
            } else if(!mNfcAdapter.isEnabled()){
                settingsCheck(getResources().getString(R.string.dis_nfc_msg), NFC_SETTINGS);
            }
        } else {
            if(!isOnline()) {
                settingsCheck(getResources().getString(R.string.no_internet), GENERAL_SETTINGS);
            }
        }

        handleIntent(getIntent());
    }

    /*method for checking if there is an active internet connection*/
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /*method for showing a dialog window which prompts the user to activate nfc and/or wireless*/
    private void settingsCheck(String title, final int settings){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(title)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.positive), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        if (settings == GENERAL_SETTINGS) {
                            startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                        } else if (settings == NFC_SETTINGS) {
                            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                        }

                    }
                })
                .setNegativeButton(getResources().getString(R.string.negative), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /*Method for handling the intent*/
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }


    /* This method gets called, when a new Intent gets associated with the current activity instance.
      Instead of creating a new activity, onNewIntent will be called. For more information have a look
      at the documentation. In our case this method gets called, when the user attaches a Tag to the device.*/
    @Override
    protected void onNewIntent(Intent intent) {

        handleIntent(intent);
    }

    /*Asynchronous task for reading the data.*/
     private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {

         /* bit_7 defines encoding
            bit_6 reserved for future use, must be 0
            bit_5..0 length of IANA language code
            See NFC forum specification for "Text Record Type Definition" at 3.2.1
             http://www.nfc-forum.org/specs/
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 63;

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                tag_id = result;
                setContentView(R.layout.activity_main);
                initInfoLayout();
                tag.setText(getResources().getString(R.string.taginfo) + "  " + result);
                new QueryTask().execute(result);
            }
        }
    }

    /*method for initializing the layout parameters. Created to simplify other code blocks*/
    private void initInfoLayout(){

        tag = (TextView) findViewById(R.id.tvitem);
        type = (TextView) findViewById(R.id.type);
        model  = (TextView) findViewById(R.id.model);
        sn  = (TextView) findViewById(R.id.sn);
        st  = (TextView) findViewById(R.id.service);
        supplier  = (TextView) findViewById(R.id.supplier);
        specs  = (TextView) findViewById(R.id.specs);
        date  = (TextView) findViewById(R.id.date);
        producer  = (TextView) findViewById(R.id.producer);
        cost  = (TextView) findViewById(R.id.cost);
        warranty  = (TextView) findViewById(R.id.warranty);
        receipt  = (TextView) findViewById(R.id.receipt);
        location  = (TextView) findViewById(R.id.location);
    }

    /*Asynchronous Task for querying the tapped item*/
    private class QueryTask extends AsyncTask<String,Void,Integer>{

        private Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        private DBCon cn;

        //showing the progress dialog window
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle(getResources().getString(R.string.wait));
            progressDialog.setMessage(getResources().getString(R.string.search));
            progressDialog.setCancelable(false);
            //set the dialog cancelable and closing on cancel button press
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                QueryTask.this.cancel(true);    //cancel the async task
                progressDialog.dismiss();       //close progress dialog
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.canceled_operation), Toast.LENGTH_SHORT).show();
            }
            });
            progressDialog.show();

        }

        @Override
        protected Integer doInBackground(String...params) {
            try {
                cn = new DBCon();
                conn = cn.getCon();     //getting the connection
                //statement to search for items with a specific serial number
                ps = conn.prepareStatement("Select * from attributes where serialNum = ?");
                ps.setString(1, params[0]);
                rs = ps.executeQuery();     //execute query


            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Integer result){

            progressDialog.dismiss();       //closing the progress dialog window

            try {
                if((rs.next())){        //if query returned something

                    type.setText(rs.getString(2));
                    model.setText(rs.getString(3));
                    producer.setText(rs.getString(4));
                    specs.setText(rs.getString(5));
                    sn.setText(rs.getString(6));
                    st.setText(rs.getString(7));
                    cost.setText(rs.getString(8));
                    warranty.setText(rs.getString(9));
                    receipt.setText(rs.getString(10));
                    supplier.setText(rs.getString(11));
                    DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
                    Date dt = rs.getDate(12);
                    date.setText(df.format(dt));
                    location.setText(rs.getString(13));

                    //close everything
                    ps.close();
                    rs.close();
                    cn.closeCon(conn);

                }
                else{       //if query returned nothing then reset the layout

                    type.setText("");
                    model.setText("");
                    producer.setText("");
                    specs.setText("");
                    sn.setText("");
                    st.setText("");
                    cost.setText("");
                    warranty.setText("");
                    receipt.setText("");
                    supplier.setText("");
                    date.setText("");
                    location.setText("");

                    //close everything
                    ps.close();
                    rs.close();
                    cn.closeCon(conn);

                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.not_found) + "  " + tag_id, Toast.LENGTH_LONG).show();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    /*parameter activity refers to the corresponding MainActivity requesting the foreground dispatch.
      and parameter adapter refers to the NfcAdapter used for the foreground dispatch.*/
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /*parameter activity refers to he corresponding MainActivity requesting to stop the foreground dispatch.
      and parameter adapter refers to the NfcAdapter used for the foreground dispatch.*/
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id){
            case R.id.action_add:
                if(!isOnline()){        //if not online prompt to activate an internet connection
                    settingsCheck(getResources().getString(R.string.no_internet), GENERAL_SETTINGS);
                    return true;
                }
                id_action = ADD_ITEM;
                if(!logged_in){
                    showLogin();    //if user not logged in then show login
                }
                else{
                    addItem();      //if user already logged then call additem method
                }
                return true;
            case R.id.action_show:
                if(!isOnline()){        //if not online prompt to activate an internet connection
                    settingsCheck(getResources().getString(R.string.no_internet), GENERAL_SETTINGS);
                    return true;
                }
                id_action = SHOW_DB;
                if(!logged_in){
                    showLogin();        //if user not logged in then show login
                }
                else{
                    showdb();           //if user already logged then call showdb method
                }
                return true;
            case R.id.action_about:
                getAbout();
                return true;
            case R.id.action_help:
                getHelp();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*method for preparing the intent for showing the about dialog window*/
    private void getAbout(){
        Intent i = new Intent("com.gmarelas.uthlabsequipment.ABOUT");
        startActivity(i);
    }

    /*method for preparing the intent for showing the help dialog window*/
    private void getHelp(){
        Intent i = new Intent("com.gmarelas.uthlabsequipment.HELP");
        startActivity(i);
    }

    /*method for preparing the intent for showing the database*/
    private void showdb(){
        Intent i = new Intent(this, ShowDB.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    /*method for preparing the intent for Adding an item and adding to the extras the added_by parameter*/
    private void addItem() {
        Intent i = new Intent(this, AddItem.class);
        i.putExtra("added_by", added_by);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    /*method for showing the login dialog window*/
    private void showLogin(){

        final EditText txtUsername, txtPassword;
        final Button btnLogin, btnCancel;

        login = new Dialog(MainActivity.this);
        login.setContentView(R.layout.login_dialog);

        btnLogin = (Button) login.findViewById(R.id.btnLogin);
        btnCancel = (Button) login.findViewById(R.id.btnCancel);
        txtUsername = (EditText)login.findViewById(R.id.txtUsername);
        txtPassword = (EditText)login.findViewById(R.id.txtPassword);

        login.setTitle(getResources().getString(R.string.ecred));

        //add listeners for the login and cancel button

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (txtUsername.getText().toString().trim().length() > 0 && txtPassword.getText().toString().trim().length() > 0) {
                    //execute the login task
                    new LoginTask().execute(txtUsername.getText().toString(), txtPassword.getText().toString());
                } else {
                    Toast.makeText(MainActivity.this,
                            "Please enter Username and Password", Toast.LENGTH_LONG).show();

                }
            }

        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login.dismiss();
            }
        });

        login.show();

    }

    /*Asynchronous Task for logging in to the database for further operations like add and show*/
    private class LoginTask extends AsyncTask<String,Void,Boolean>{

        private PreparedStatement ps = null;
        private ResultSet rs = null;
        private Connection conn = null;
        private boolean status = false;
        private DBCon cn;

        //showing the progress dialog window
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle(getResources().getString(R.string.wait));
            progressDialog.setMessage(getResources().getString(R.string.auth));
            progressDialog.setCancelable(false);
            //set the dialog cancelable and closing on cancel button press
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LoginTask.this.cancel(true);    //cancel the async task
                    progressDialog.dismiss();       //close progress dialog
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.canceled_operation), Toast.LENGTH_SHORT).show();
                }
            });
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String...params) {

            try {
                cn = new DBCon();
                conn = cn.getCon();     //getting the connection
                //statement to query for a specific user
                ps = conn.prepareStatement("Select * from users where username = ? and password = ?");
                ps.setString(1, params[0]);
                ps.setString(2, params[1]);
                rs = ps.executeQuery();     //executing the query
                status = rs.next();
                if(status){             //if the user exists
                    added_by = params[0];
                    logged_in = true;       //make logged_in true so the user does not need to re-login during an session
                }
                //close everything
                rs.close();
                ps.close();
                cn.closeCon(conn);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return status;
        }

        @Override
        protected void onPostExecute(Boolean result){

            super.onPostExecute(result);
            progressDialog.dismiss();       //closing the progree dialog window

            if(!result){
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.wrong_pass), Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(MainActivity.this,
                        getResources().getString(R.string.right_pass), Toast.LENGTH_LONG).show();
                login.dismiss();        //closing the login window
                //depending on the option selected call the proper method
               if(id_action == ADD_ITEM){
                   addItem();
               }
                else if(id_action == SHOW_DB){
                   showdb();
               }
            }

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        /* It's important, that the activity is in the foreground (resumed). Otherwise an IllegalStateException is thrown.*/

        setupForegroundDispatch(this, mNfcAdapter);
    }

    /*overriding onPause and closing progress dialog window to avoid window leaks*/
    @Override
    protected void onPause() {

        /* Call this before onPause, otherwise an IllegalArgumentException is thrown as well. */

        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    /*overriding onPause and closing progress dialog window to avoid window leaks*/
    @Override
    protected void onStop() {

        progressDialog.dismiss();

        super.onStop();
    }


}
