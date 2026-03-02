package tn.esprit.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class MyDB {
    private final String URL = "jdbc:mysql://127.0.0.1:3306/fintech";
    private final String USERNAME = "root";
    private final String PWD = "";

    public static MyDB instance;

    private Connection conx;

    private MyDB(){
        try {
            conx = DriverManager.getConnection(URL,USERNAME,PWD);
            System.out.println("Connected to DB!");
        } catch (SQLException e) {
            System.out.println("⚠️  DB connection failed: " + e.getMessage());
            System.out.println("   Make sure MySQL is running on 127.0.0.1:3306 with database 'fintech'.");
        }
    }

    public static MyDB getInstance(){
        if (instance == null){
            instance = new MyDB();
        }
        return instance;
    }


    public Connection getConx() {
        return conx;
    }

    public String getUsername() { return USERNAME; }
    public String getPassword()  { return PWD; }
    public String getUrl()       { return URL; }
}
