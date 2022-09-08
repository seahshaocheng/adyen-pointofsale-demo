package com.example.adyenpointofsale;

import static java.lang.System.currentTimeMillis;
import static java.security.AccessController.getContext;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.enums.Environment;
import com.adyen.model.nexo.AmountsReq;
import com.adyen.model.nexo.MessageCategoryType;
import com.adyen.model.nexo.MessageClassType;
import com.adyen.model.nexo.MessageHeader;
import com.adyen.model.nexo.MessageType;
import com.adyen.model.nexo.PaymentRequest;
import com.adyen.model.nexo.PaymentTransaction;
import com.adyen.model.nexo.SaleData;
import com.adyen.model.nexo.SaleToPOIRequest;
import com.adyen.model.nexo.TransactionIdentification;
import com.adyen.model.posterminalmanagement.GetStoresUnderAccountResponse;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountRequest;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountResponse;
import com.adyen.model.posterminalmanagement.MerchantAccount;
import com.adyen.model.posterminalmanagement.Store;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.model.terminal.security.SecurityKey;
import com.adyen.service.PosTerminalManagement;
import com.adyen.service.TerminalCloudAPI;
import com.adyen.service.TerminalLocalAPI;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.codec.binary.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.security.Timestamp;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
public class MainActivity extends AppCompatActivity {

    public float orderTotal = 0;
    private String api_key=null;
    private String merchant_account=null;
    private String company_account = null;
    private String local_crypto_version=null;
    private String local_key_version=null;
    private String local_key_identifier=null;
    private String local_key_phrase=null;
    private ArrayList<CartItem> cart_lists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Demo Home");
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //Printing current WIFI connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        //Getting preference
        this.refreshReferences();

        if(this.merchant_account!=null && this.api_key!=null && this.company_account!=null){
        }
        else{
            Toast.makeText(getApplicationContext(), "Please complete application set up", Toast.LENGTH_LONG).show();
            Intent i = new Intent(MainActivity.this, DemoConfiguration.class);
            startActivity(i);
        }
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.settings) {
            // do something here
            Intent i = new Intent(MainActivity.this, DemoConfiguration.class);
            startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshReferences(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.company_account = prefs.getString("company_account",null);
        this.merchant_account = prefs.getString("merchant_account",null);
        this.api_key = prefs.getString("api_key",null);
        this.local_crypto_version = prefs.getString("crypto_version",null);
        this.local_key_identifier = prefs.getString("key_identifier",null);
        this.local_key_phrase = prefs.getString("key_phrase",null);
        this.local_key_version = prefs.getString("key_version",null);
    }

    private SaleToPOIRequest generatePOIRequest() {
        Random rnd = new Random();
        int number = rnd.nextInt(9999);

        //Get POI ID
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //Spinner availableSpinner = (Spinner)findViewById(R.id.availableTerminals);
        //String selectedPOI = availableSpinner.getSelectedItem().toString();
        String selectedPOI = prefs.getString("pairedTerminal",null);
        String POSID = prefs.getString("pos_id",null);
        String reference_prefix = prefs.getString("reference_prefix",null);
        Long tsLong = System.currentTimeMillis()/1000;

        String saleID = POSID;
        String serviceID = tsLong.toString();
        String POIID = selectedPOI;
        String transactionID = reference_prefix+"_"+Integer.toString(number);

        SaleToPOIRequest saleToPOIRequest = new SaleToPOIRequest();
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setProtocolVersion("3.0");
        messageHeader.setMessageClass( MessageClassType.SERVICE );
        messageHeader.setMessageCategory( MessageCategoryType.PAYMENT );
        messageHeader.setMessageType( MessageType.REQUEST );
        messageHeader.setSaleID(saleID);
        messageHeader.setServiceID(serviceID);
        messageHeader.setPOIID(POIID);
        saleToPOIRequest.setMessageHeader(messageHeader);

        PaymentRequest paymentRequest = new PaymentRequest();
        SaleData saleData = new SaleData();
        TransactionIdentification saleTransactionID = new TransactionIdentification();
        saleTransactionID.setTransactionID(transactionID);
        try {
            saleTransactionID.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        saleData.setSaleTransactionID(saleTransactionID);
        paymentRequest.setSaleData(saleData);

        PaymentTransaction paymentTransaction = new PaymentTransaction();
        AmountsReq amountsReq = new AmountsReq();
        amountsReq.setCurrency("MYR");
        amountsReq.setRequestedAmount( BigDecimal.valueOf(orderTotal) );
        paymentTransaction.setAmountsReq(amountsReq);
        paymentRequest.setPaymentTransaction(paymentTransaction);
        saleToPOIRequest.setPaymentRequest(paymentRequest);

        return saleToPOIRequest;
    }

    public void AddToCart(View view) {
        switch (view.getId()) {
            case (R.id.plus1):
                //stuff
                orderTotal+=1;
                break;
            case (R.id.plus2):
                //stuff
                orderTotal+=2;
                break;
            case (R.id.plus4):
                //stuff
                orderTotal+=4;
                break;
            case (R.id.plus5):
                //stuff
                orderTotal+=5;
                break;
        }
        TextView total = (TextView) findViewById(R.id.totalField);
        total.setText(Float.toString(orderTotal));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void makeLocalPayment(String localIPAddress) throws FileNotFoundException, CertificateException {
        Button paymentButton = (Button) findViewById(R.id.paymentButton);
        this.refreshReferences();

        if(this.local_crypto_version==null || this.local_key_version==null || this.local_key_phrase==null || this.local_key_identifier==null){
            Toast.makeText(getApplicationContext(), "Please complete local integration set up", Toast.LENGTH_LONG).show();
            Intent i = new Intent(MainActivity.this, DemoConfiguration.class);
            startActivity(i);
        }

        Log.e("Info","Initiating Local Payment");
        Config config = new Config();
        config.setMerchantAccount(this.merchant_account);
        config.setTerminalCertificate(getResources().openRawResource(R.raw.adyen_terminalfleet_test));
        config.setTerminalApiLocalEndpoint("https://"+localIPAddress);

        SecurityKey securityKey = new SecurityKey();
        securityKey.setAdyenCryptoVersion(Integer.valueOf(this.local_crypto_version));
        securityKey.setKeyIdentifier(this.local_key_identifier);
        securityKey.setPassphrase(this.local_key_phrase);
        securityKey.setKeyVersion(Integer.valueOf(this.local_key_version));

        Client terminalLocalAPIClient = new Client(config);
        terminalLocalAPIClient.setEnvironment(Environment.TEST,null);
        TerminalLocalAPI terminalLocalAPI = new TerminalLocalAPI(terminalLocalAPIClient);
        SaleToPOIRequest saleToPOIRequest =  generatePOIRequest();

        TerminalAPIRequest terminalApiRequest = new TerminalAPIRequest();
        terminalApiRequest.setSaleToPOIRequest(saleToPOIRequest);
        try {
            Toast.makeText(getApplicationContext(), "Connected to terminal locally, make payment", Toast.LENGTH_SHORT).show();
            TerminalAPIResponse terminalAPIResponse = terminalLocalAPI.request(terminalApiRequest, securityKey);

            if(terminalAPIResponse.getSaleToPOIResponse()!=null){

                Log.i("Info","The result"+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult());
                Log.i("Info","The result"+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse());
                Log.i("Info:","Completed Local Connection!");

                byte[] decoded = Base64.getDecoder().decode(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse());
                String resultCode = terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult().toString();

                if(resultCode.equalsIgnoreCase("success")){
                    Toast.makeText(getApplicationContext(), "Payment is successful", Toast.LENGTH_SHORT).show();
                    paymentButton.setEnabled(true);
                }
                else{
                    JSONObject additionalData = new JSONObject(new String(decoded));
                    String message = additionalData.getString("message");
                    String refusalReason = additionalData.getString("refusalReason");
                    Toast.makeText(getApplicationContext(), "Payment failed: "+ message +"::"+refusalReason, Toast.LENGTH_LONG).show();
                    paymentButton.setEnabled(true);
                }
            }
            else{
                Toast.makeText(getApplicationContext(), "Unable to establish connection with terminal locally", Toast.LENGTH_LONG).show();
                paymentButton.setEnabled(true);
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION!");
            Toast.makeText(getApplicationContext(), "Something went wrong when connecting to terminal", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void makeCloudPayment() {
        Button paymentButton = (Button) findViewById(R.id.paymentButton);
        Log.e("Info","Initiating Cloud Payment");

        Config config = new Config();
        config.setMerchantAccount(this.merchant_account);
        config.setApiKey(this.api_key);

        Client terminalCloudAPIClient = new Client(config);
        terminalCloudAPIClient.setEnvironment(Environment.TEST,null);
        TerminalCloudAPI terminalCloudAPI = new TerminalCloudAPI(terminalCloudAPIClient);

        SaleToPOIRequest saleToPOIRequest =  generatePOIRequest();
        TerminalAPIRequest terminalApiRequest = new TerminalAPIRequest();
        terminalApiRequest.setSaleToPOIRequest(saleToPOIRequest);

        try {

            TerminalAPIResponse terminalAPIResponse = terminalCloudAPI.sync(terminalApiRequest);
            if(terminalAPIResponse.getSaleToPOIResponse()!= null){
                byte[] decoded = Base64.getDecoder().decode(terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse());
                String resultCode = terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult().toString();

                Log.i("Info","The resultCode: "+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult());
                Log.i("Info","The additionalData"+new String(decoded));
                Log.i("Info:","Completed Cloud Connection!");

                if(resultCode.equalsIgnoreCase("success")){
                    Toast.makeText(getApplicationContext(), "Payment is successful", Toast.LENGTH_SHORT).show();
                }
                else{
                    JSONObject additionalData = new JSONObject(new String(decoded));
                    String message = additionalData.getString("message");
                    String refusalReason = additionalData.getString("refusalReason");
                    Toast.makeText(getApplicationContext(), "Payment failed: "+ message +"::"+refusalReason, Toast.LENGTH_LONG).show();
                }
                paymentButton.setEnabled(true);
            }else{
                if(terminalAPIResponse.getSaleToPOIRequest().getEventNotification()!= null){
                    Toast.makeText(getApplicationContext(), terminalAPIResponse.getSaleToPOIRequest().getEventNotification().getEventDetails(), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), "Unable to establish connection with terminal via cloud", Toast.LENGTH_LONG).show();
                paymentButton.setEnabled(true);
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION!");
            Toast.makeText(getApplicationContext(), "Something went wrong when connecting to terminal", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void makePayment(View view) throws FileNotFoundException, CertificateException {
        Button paymentButton = (Button) findViewById(R.id.paymentButton);
        paymentButton.setEnabled(false);
        Switch goLocal = (Switch) findViewById(R.id.goLocal);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String localIP = prefs.getString("localIP",null);
        TextView localIPField = (TextView) findViewById(R.id.LocalIPAddress);
        Toast.makeText(getApplicationContext(), "Connecting to terminal", Toast.LENGTH_SHORT).show();
       if(goLocal.isChecked()){
            Log.i("Info","Going Local");
            this.makeLocalPayment(localIP);
        }
        else{
            this.makeCloudPayment();
        }
    }

    public void clickSetting(View view){
        Intent i = new Intent(MainActivity.this, DemoConfiguration.class);
        startActivity(i);
    }
}