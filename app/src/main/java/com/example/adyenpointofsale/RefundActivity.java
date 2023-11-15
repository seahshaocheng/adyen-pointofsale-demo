package com.example.adyenpointofsale;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
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
import com.adyen.model.nexo.OriginalPOITransaction;
import com.adyen.model.nexo.PaymentRequest;
import com.adyen.model.nexo.PaymentTransaction;
import com.adyen.model.nexo.ReversalReasonType;
import com.adyen.model.nexo.ReversalRequest;
import com.adyen.model.nexo.SaleData;
import com.adyen.model.nexo.SaleToPOIRequest;
import com.adyen.model.nexo.TransactionIdentification;
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.model.terminal.security.SecurityKey;
import com.adyen.service.TerminalCloudAPI;
import com.adyen.service.TerminalLocalAPI;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class RefundActivity extends AppCompatActivity {

    private String api_key=null;
    private String merchant_account=null;
    private String company_account = null;
    private String local_crypto_version=null;
    private String local_key_version=null;
    private String local_key_identifier=null;
    private String local_key_phrase=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refund);


        Bundle extras = getIntent().getExtras();
        String value = extras.getString("source");
        Log.i("source",value);
        if(value != null && value.equals("scanner")){
            Log.i("msg","gettingExtras");
            String pspReference = extras.getString("pspReference");
            EditText pspReferenceText = (EditText) findViewById(R.id.pspReference);
            pspReferenceText.setText(pspReference);
        }

        Log.i("type",value);
        this.refreshReferences();
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
            Intent i = new Intent(RefundActivity.this, DemoConfiguration.class);
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

    private SaleToPOIRequest generatePOIRequest(String transaction_id,Double refund_amount) {
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
        String currency = prefs.getString("tender_currency",null);

        String saleID = POSID;
        String serviceID = tsLong.toString();
        String POIID = selectedPOI;
        String transactionID = reference_prefix+"_"+Integer.toString(number);

        SaleToPOIRequest saleToPOIRequest = new SaleToPOIRequest();
        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setProtocolVersion("3.0");
        messageHeader.setMessageClass( MessageClassType.SERVICE );
        messageHeader.setMessageCategory( MessageCategoryType.REVERSAL );
        messageHeader.setMessageType( MessageType.REQUEST );
        messageHeader.setSaleID(saleID);
        messageHeader.setServiceID(serviceID);
        messageHeader.setPOIID(POIID);
        saleToPOIRequest.setMessageHeader(messageHeader);

        ReversalRequest refundRequest = new ReversalRequest();
        OriginalPOITransaction originalTransaction = new OriginalPOITransaction();
        TransactionIdentification POITransctionID = new TransactionIdentification();
        try {
            POITransctionID.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        POITransctionID.setTransactionID(transaction_id);
        originalTransaction.setPOITransactionID(POITransctionID);
        refundRequest.setOriginalPOITransaction((originalTransaction));
        refundRequest.setReversalReason(ReversalReasonType.valueOf("MERCHANT_CANCEL"));

        if(refund_amount>0){
            BigDecimal convertedRefundAmount = new BigDecimal(refund_amount);
            refundRequest.setReversedAmount(convertedRefundAmount);
        }
        saleToPOIRequest.setReversalRequest(refundRequest);

        return saleToPOIRequest;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void refund(View view) throws FileNotFoundException, CertificateException {
        Button refundButton = (Button) findViewById(R.id.refundActButton);
        refundButton.setEnabled(false);
        Switch goLocal = (Switch) findViewById(R.id.goLocal);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String localIP = prefs.getString("localIP",null);
        TextView localIPField = (TextView) findViewById(R.id.LocalIPAddress);
        Toast.makeText(getApplicationContext(), "Connecting to terminal", Toast.LENGTH_SHORT).show();

        Config config = new Config();
        config.setMerchantAccount(this.merchant_account);
        config.setApiKey(this.api_key);

        //Refund ID
        EditText pspReference = (EditText) findViewById(R.id.pspReference);
        String refundTxnId = "."+pspReference.getText().toString();

        //Refund Amount
        EditText refundAmountBox = (EditText) findViewById(R.id.refund_amount);
        String refundAmountString = refundAmountBox.getText().toString();
        double refundAmount =0.0;
        try {
            refundAmount = Double.parseDouble(refundAmountString);
            // Now 'refundAmount' contains the user-inputted double value
            // You can use this value as needed
        } catch (NumberFormatException e) {
            // Handle the case where the input is not a valid double
            Toast.makeText(this, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show();
        }

        TerminalAPIResponse terminalAPIResponse = null;
        SaleToPOIRequest saleToPOIRequest =  generatePOIRequest(refundTxnId,refundAmount);
        TerminalAPIRequest terminalApiRequest = new TerminalAPIRequest();
        terminalApiRequest.setSaleToPOIRequest(saleToPOIRequest);

        try {
            if(goLocal.isChecked()){
                config.setTerminalCertificate(getResources().openRawResource(R.raw.adyen_terminalfleet_test));
                config.setTerminalApiLocalEndpoint("https://"+localIP);

                SecurityKey securityKey = new SecurityKey();
                securityKey.setAdyenCryptoVersion(Integer.valueOf(this.local_crypto_version));
                securityKey.setKeyIdentifier(this.local_key_identifier);
                securityKey.setPassphrase(this.local_key_phrase);
                securityKey.setKeyVersion(Integer.valueOf(this.local_key_version));
                Log.i("Info","Going Local");
                Client terminalLocalAPIClient = new Client(config);
                terminalLocalAPIClient.setEnvironment(Environment.TEST,null);

                TerminalLocalAPI terminalLocalAPI = new TerminalLocalAPI(terminalLocalAPIClient);
                terminalAPIResponse = terminalLocalAPI.request(terminalApiRequest, securityKey);
            }
            else{
                Client terminalCloudAPIClient = new Client(config);
                terminalCloudAPIClient.setEnvironment(Environment.TEST,null);
                TerminalCloudAPI terminalCloudAPI = new TerminalCloudAPI(terminalCloudAPIClient);
                terminalAPIResponse = terminalCloudAPI.sync(terminalApiRequest);
            }

            if(terminalAPIResponse.getSaleToPOIResponse()!= null){
                byte[] decoded = Base64.getDecoder().decode(terminalAPIResponse.getSaleToPOIResponse().getReversalResponse().getResponse().getAdditionalResponse());
                String resultCode = terminalAPIResponse.getSaleToPOIResponse().getReversalResponse().getResponse().getResult().toString();

                Log.i("Info","The resultCode: "+terminalAPIResponse.getSaleToPOIResponse().getReversalResponse().getResponse().getResult());
                Log.i("Info","The additionalData"+new String(decoded));
                Log.i("Info:","Completed Cloud Connection!");

                if(resultCode.equalsIgnoreCase("success")){
                    Toast.makeText(getApplicationContext(), "Refund Request is successful", Toast.LENGTH_SHORT).show();
                }
                else{
                    JSONObject additionalData = new JSONObject(new String(decoded));
                    String message = additionalData.getString("message");
                    String refusalReason = additionalData.getString("refusalReason");
                    Toast.makeText(getApplicationContext(), "Payment Request failed: "+ message +"::"+refusalReason, Toast.LENGTH_LONG).show();
                }
                refundButton.setEnabled(true);
            }else{
                if(terminalAPIResponse.getSaleToPOIRequest().getEventNotification()!= null){
                    Toast.makeText(getApplicationContext(), terminalAPIResponse.getSaleToPOIRequest().getEventNotification().getEventDetails(), Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getApplicationContext(), "Unable to establish connection with terminal via cloud", Toast.LENGTH_LONG).show();
                refundButton.setEnabled(true);
            }
        } catch (Exception e) {
            System.out.println("EXCEPTION!");
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            refundButton.setEnabled(true);
        }

    }

    public void refundScan(View view){
        Intent i = new Intent(RefundActivity.this, ScannerActivity.class);
        i.putExtra("source","refund");
        startActivity(i);
    }
}