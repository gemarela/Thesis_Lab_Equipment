package com.gmarelas.uthlabsequipment;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*class for showing the whole database. Only the model and the serial number are shown.*/
public class ShowDB extends ListActivity {

    public static final int ACTIVITY_CREATE = 1;
    private List<HashMap<String,String>> list_items;
    private HashMap<String, String> item;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showdb);

        progressDialog = new ProgressDialog(ShowDB.this);

        registerForContextMenu(getListView());
        new ShowTask().execute();
    }


    private class ShowTask extends AsyncTask<Void,Void,Integer> {

        private PreparedStatement ps = null;
        private Connection conn = null;
        private ResultSet rs = null;
        private DBCon cn;
        private String[] from = new String[] { "item", "serialn" };     //setting the id keys
        private int[] to = new int[] { R.id.item, R.id.serial_num };    //setting the destination in the list_item

        //showing the progress dialog window
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            progressDialog = new ProgressDialog(ShowDB.this);
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
        protected Integer doInBackground(Void...params) {
            try {
                list_items = new ArrayList<>();
                cn = new DBCon();
                conn = cn.getCon();     //getting the connection
                //statement for selecting all entries in the database
                ps = conn.prepareStatement("Select * from attributes");
                rs = ps.executeQuery();     //executing the query
                while(rs.next()){       //while there is an item in the database
                    item = new HashMap<>();     //create new HashMap for every item
                    item.put("item", rs.getString(3));      //insert model paired with key item
                    item.put("serialn", rs.getString(6));   //insert serial number paired with key serialn
                    list_items.add(item);       //adding each item to the list
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        protected void onPostExecute(Integer result){
            progressDialog.dismiss();       //closing the progress dialog window
            //creating an adapter for the items
            SimpleAdapter adapter = new SimpleAdapter(ShowDB.this, list_items, R.layout.list_item, from, to);
            setListAdapter(adapter);        //showing the list
            try {
                //close everything
                ps.close();
                rs.close();
                cn.closeCon(conn);
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }

    }

    /*method for returning the item selected*/
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        return super.onContextItemSelected(item);
    }

    /*method for getting the item selected*/
    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        Intent i = new Intent(this, ShowItem.class);
        item = list_items.get(pos);
        i.putExtra("sn", item.get("serialn"));
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    /*inflate the settings menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.info_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*triggered actions for option selected*/
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
