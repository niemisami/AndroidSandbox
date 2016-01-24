package com.niemisami.androidsandbox.Web;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.niemisami.androidsandbox.R;

import org.w3c.dom.Text;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple {@link Fragment} subclass.
 */
public class WebFragment extends Fragment {

    public static final String TAG = "Basic Network Demo";
    // Whether there is a Wi-Fi connection.
    private static boolean mWifiConnected = false;
    // Whether there is a mobile connection.
    private static boolean mMobileConnected = false;

    private TextView mWebHtmlView;


    public WebFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkNetworkConnection();

//        new DownloadTask().execute("http://pikkulaskiainen.fi/tapahtuma/#Aikataulu");
//        new DownloadTask().execute("http://data.foli.fi/siri/vm");
//        new DownloadTask().execute("http://www.junat.net/fi/");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        mWebHtmlView = (TextView) view.findViewById(R.id.webHtmlView);



        return view;

    }


    ///////NETWORKING////////

//    region

    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
        if(activeInfo != null && activeInfo.isConnected()) {
            mWifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mMobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            if(mWifiConnected) {
                Log.d(TAG, "wifi: " + mWifiConnected + " mobile: " + mMobileConnected);
                Toast.makeText(getActivity().getApplicationContext(), "Wifi is connected", Toast.LENGTH_SHORT).show();
            } else if (mMobileConnected) {
                Toast.makeText(getActivity().getApplicationContext(), "Mobile data connected", Toast.LENGTH_SHORT).show();
            }

            new DownloadTask().execute("http://teekkariristeily.net/prices.html");

        } else {
            Toast.makeText(getActivity().getApplicationContext(), "No network connection available", Toast.LENGTH_SHORT).show();
        }
    }


    private InputStream downloadUri(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        connection.connect();

        InputStream stream = connection.getInputStream();
        return stream;
    }

    private String readInputStream(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;

        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
            mWebHtmlView.setText(s);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                return loadFromNetwork(params[0]);
            } catch (IOException e) {
                Log.e(TAG, "Task error", e);
                return "Error with loading network";
            }
        }
    }

    private String loadFromNetwork(String urlString) throws IOException {
        InputStream stream;
        String str;

        stream = downloadUri(urlString);
        try {
            str = readInputStream(stream,2500);
        } finally {
            if(stream != null) {
                stream.close();
            }
        }
        return str;
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
//    endregion

}
