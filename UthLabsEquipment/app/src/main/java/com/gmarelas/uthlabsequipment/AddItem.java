package com.gmarelas.uthlabsequipment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.GregorianCalendar;

public class AddItem extends Activity{

    private static final int LENGTH1 = 35;      //length of some attributes in the database
    private static final int LENGTH2 = 15;      //length of some attributes in the database
    private static final int LENGTH3 = 200;     //length of some attributes in the database
    private EditText type, model, sn, st, supplier, specs, producer, cost, warranty, receipt, location;
    private DatePicker date;
    private ProgressDialog progressDialog;
    private String added_by;                    //helper variable to store the user who added an item to the database
    static int status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_item);

        progressDialog = new ProgressDialog(AddItem.this);

        type = (EditText) findViewById(R.id.type);
        model  = (EditText) findViewById(R.id.model);
        sn  = (EditText) findViewById(R.id.sn);
        st  = (EditText) findViewById(R.id.service);
        supplier  = (EditText) findViewById(R.id.supplier);
        specs  = (EditText) findViewById(R.id.specs);
        producer  = (EditText) findViewById(R.id.producer);
        cost  = (EditText) findViewById(R.id.cost);
        warranty  = (EditText) findViewById(R.id.warranty);
        receipt  = (EditText) findViewById(R.id.receipt);
        location  = (EditText) findViewById(R.id.location);
        date = (DatePicker) findViewById(R.id.date_widget);

        Bundle extras = getIntent().getExtras();
        if(extras!=null){       //getting the extras
            added_by = extras.getString("added_by");
        }

    }

    /*inflate the options menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*actions triggered on option selection*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if(!save()){
                    return false;
                }
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*Asynchronous task for saving item to the database*/
    private class SaveTask extends AsyncTask<String,Void,Integer>{

        private Connection conn = null;
        private PreparedStatement ps = null;
        private DBCon cn;

        //showing the progress dialog
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressDialog = new ProgressDialog(AddItem.this);
            progressDialog.setTitle(getResources().getString(R.string.wait));
            progressDialog.setMessage(getResources().getString(R.string.saving));
            progressDialog.setCancelable(false);
            //set the dialog cancelable and closing on cancel button press
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SaveTask.this.cancel(true);    //cancel the async task
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
                conn = cn.getCon();     //getting the connection with the database
                //creating the sql expression for inserting to the database
                ps = conn.prepareStatement("Insert into attributes values(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                ps.setString(1,type.getText().toString());
                ps.setString(2,model.getText().toString());
                ps.setString(3,producer.getText().toString());
                ps.setString(4,specs.getText().toString());
                ps.setString(5,sn.getText().toString());
                ps.setString(6,st.getText().toString());
                ps.setString(7, cost.getText().toString());
                ps.setString(8, warranty.getText().toString());
                ps.setString(9, receipt.getText().toString());
                ps.setString(10, supplier.getText().toString());
                int day = date.getDayOfMonth();
                int month = date.getMonth() + 1;
                int year = date.getYear();
                //getting the date
                GregorianCalendar gdt = new GregorianCalendar(year,month,day);
                //convert GregorianCalendar to sql.Date object
                java.sql.Date dt = new java.sql.Date(gdt.getTimeInMillis());
                ps.setDate(11,dt);
                ps.setString(12, location.getText().toString());
                ps.setString(13, params[0]);
                status = ps.executeUpdate();        //executing the update

            } catch (Exception e) {
                e.printStackTrace();
            }
            return status;
        }

        protected void onPostExecute(Integer result){
            progressDialog.dismiss();       //closing the progress dialog window
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            cn.closeCon(conn);      //closing the connection
        }

    }

    /*method for checking validity of fields and saving the item*/
    private boolean save(){

        //helper variables for checking validity of fields
        boolean type_check, model_check, prod_check, supp_check, loc_check, specs_check, sn_check, st_check, cost_check, warr_check, rec_check;

        type_check = model_check = prod_check = supp_check = loc_check = specs_check = sn_check = st_check = cost_check = warr_check = rec_check = true;

        if(type.getText().toString().length()>LENGTH1 || type.getText().toString().equals("")){
            type_check = false;
            type.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(model.getText().toString().length()>LENGTH1 || model.getText().toString().equals("")){
            model_check = false;
            model.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(producer.getText().toString().length()>LENGTH1 || producer.getText().toString().equals("")){
            prod_check = false;
            producer.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(supplier.getText().toString().length()>LENGTH1 || supplier.getText().toString().equals("")){
            supp_check = false;
            supplier.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(location.getText().toString().length()>LENGTH1 || location.getText().toString().equals("")){
            loc_check = false;
            location.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(specs.getText().toString().length()>LENGTH3 || specs.getText().toString().equals("")){
            specs_check = false;
            specs.setError(getResources().getString(R.string.data_length) + " " + LENGTH3 + " " + getResources().getString(R.string.chars));
        }
        if(sn.getText().toString().length()>LENGTH1 || sn.getText().toString().equals("")){
            sn_check = false;
            sn.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(st.getText().toString().length()>LENGTH1 || st.getText().toString().equals("")){
            st_check = false;
            st.setError(getResources().getString(R.string.data_length) + " " + LENGTH1 + " " + getResources().getString(R.string.chars));
        }
        if(cost.getText().toString().length()>LENGTH2 || cost.getText().toString().equals("")){
            cost_check = false;
            cost.setError(getResources().getString(R.string.data_length) + " " + LENGTH2 + " " + getResources().getString(R.string.chars));
        }
        if(warranty.getText().toString().length()>LENGTH2 || warranty.getText().toString().equals("")){
            warr_check = false;
            warranty.setError(getResources().getString(R.string.data_length) + " " + LENGTH2 + " " + getResources().getString(R.string.chars));
        }
        if(receipt.getText().toString().length()>LENGTH2 || receipt.getText().toString().equals("")){
            rec_check = false;
            receipt.setError(getResources().getString(R.string.data_length) + " " + LENGTH2 + " " + getResources().getString(R.string.chars));
        }

        //if all fields contain valid values then save the item
        if(type_check && model_check && prod_check && supp_check && loc_check && specs_check && sn_check && st_check && cost_check && warr_check && rec_check) {
            new SaveTask().execute(added_by);
        }
        else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.wrong_values), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /*overriding onStop and closing progress dialog window to avoid window leaks*/
    @Override
    protected void onStop() {

        progressDialog.dismiss();

        super.onStop();
    }

}
