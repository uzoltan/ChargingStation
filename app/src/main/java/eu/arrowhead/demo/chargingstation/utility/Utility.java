package eu.arrowhead.demo.chargingstation.utility;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;

import java.util.List;

public final class Utility {

    private static final Gson gson = new Gson();

    private Utility(){
    }

    public static boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static <T> T fromJsonObject(String json, Class<T> parsedClass){
        return gson.fromJson(json, parsedClass);
    }

    public static <T> List<T> fromJsonArray(String json, final Class<T> parsedClass){
        return gson.fromJson(json, new ListOfJson<T>(parsedClass));
    }
}
