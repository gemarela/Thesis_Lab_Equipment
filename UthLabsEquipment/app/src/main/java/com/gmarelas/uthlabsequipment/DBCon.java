package com.gmarelas.uthlabsequipment;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBCon {

    private final static String DRIVER="com.mysql.jdbc.Driver";
    private final static String CON_STRING="jdbc:mysql://10.64.83.103/nfc_database";        //link to the database
    private final static String USERNAME="marelas";
    private final static String PASSWORD="marelas";

    private Connection connection;


    private void connect(){
        try {
            Class.forName(DRIVER);      //initializes the class and gets registered in the jdbc driver manager
            connection = DriverManager.getConnection(CON_STRING, USERNAME, PASSWORD);
        }
        catch(SQLException e){      //catching exceptions when trying to connect
        System.out.println("Exception in DBCon");
        }catch(ClassNotFoundException ex){
            Logger.getLogger(DBCon.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*public getter method for getting the connection*/
    public Connection getCon(){
        connect();
        return connection;
    }

    /*public method for closing the connection*/
    public void closeCon(Connection conn){
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
