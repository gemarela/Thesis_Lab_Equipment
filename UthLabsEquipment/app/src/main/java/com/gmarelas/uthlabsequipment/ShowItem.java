package com.gmarelas.uthlabsequipment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/*class for showing the item selected from the database list*/
public class ShowItem extends Activity {

    private ProgressDialog progressDialog;
    private TextView type, model, sn, st, supplier, specs, date, producer, cost, warranty, receipt, location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_item);
        progressDialog = new ProgressDialog(ShowItem.this);


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

        Bundle extras = getIntent().getExtras();
        if(extras!=null){       //getting the extras
            String snt = extras.getString("sn");
            new ShowTask().execute(snt);
        }
    }

    /*Asynchronous task for showing the item's info*/
    private class ShowTask extends AsyncTask<String, Void, Integer>{

        private Connection conn = null;
        private PreparedStatement ps = null;
        private ResultSet rs = null;
        private DBCon cn;

        //showing the progress dialog window
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressDialog = new ProgressDialog(ShowItem.this);
            progressDialog.setTitle(getResources().getString(R.string.wait));
            progressDialog.setMessage(getResources().getString(R.string.fetch));
            progressDialog.setCancelable(false);
            //set the dialog cancelable and closing on cancel button press
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShowTask.this.cancel(true);    //cancel the async task
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
                ps = conn.prepareStatement("Select * from attributes where serialNum = ?");
                ps.setString(1, params[0]);
                rs = ps.executeQuery();     //searching for a specific item in the database


            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPostExecute(Integer result){
            progressDialog.dismiss();       //closing progress dialog window
            try {
                if(rs.next()){      //if query returned something

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
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }

    /*inflate the settings menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.info_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*actions triggered on option selection*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                setResult(RESULT_OK);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*overriding onStop and closing progress dialog window to avoid window leaks*/
    @Override
    protected void onStop() {

        progressDialog.dismiss();

        super.onStop();
    }

}
