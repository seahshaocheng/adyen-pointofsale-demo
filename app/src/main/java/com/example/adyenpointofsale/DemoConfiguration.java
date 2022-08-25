package com.example.adyenpointofsale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.enums.Environment;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountRequest;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountResponse;
import com.adyen.model.posterminalmanagement.MerchantAccount;
import com.adyen.model.posterminalmanagement.Store;
import com.adyen.service.PosTerminalManagement;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class DemoConfiguration extends AppCompatActivity {

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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.company_account = prefs.getString("company_account",null);
        this.merchant_account = prefs.getString("merchant_account",null);
        this.api_key = prefs.getString("api_key",null);
        this.local_crypto_version = prefs.getString("crypto_version",null);
        this.local_key_identifier = prefs.getString("key_identifier",null);
        this.local_key_phrase = prefs.getString("key_phrase",null);
        this.local_key_version = prefs.getString("key_version",null);
        setContentView(R.layout.activity_demo_configuration);

        EditText localIPAddress = (EditText) findViewById(R.id.LocalIPAddress);
        localIPAddress.setText(prefs.getString("localIP",null));

        //Merchant Account
        EditText merchantAccount = (EditText) findViewById(R.id.MerchantAccountValue);
        merchantAccount.setText(this.merchant_account);

        //API Key
        EditText apiKey = (EditText) findViewById(R.id.APIKeyValue);
        apiKey.setText(this.api_key);

        //Saving Cryto Value
        EditText crytoValue = (EditText) findViewById(R.id.crytoverisonvalue);
        crytoValue.setText(this.local_crypto_version);

        //Saving local key_identifier
        EditText key_identifier = (EditText) findViewById(R.id.keyidentifiervalue);
        key_identifier.setText(this.local_key_identifier);

        //Saving local passphrase value
        EditText passphrase = (EditText) findViewById(R.id.passphrase_value);
        passphrase.setText(this.local_key_phrase);

        //saving key version value
        EditText key_version = (EditText) findViewById(R.id.key_version_value);
        key_version.setText(this.local_key_version);

        TextView currentTerminalText = (TextView) findViewById(R.id.connectedTerminalText);
        currentTerminalText.setText(prefs.getString("pairedTerminal",null));

    }

    private List<String> getTerminals(){
        List<String> inStoreTerminals = new ArrayList();

        Config config = new Config();
        config.setMerchantAccount(this.merchant_account);
        config.setApiKey(this.api_key);
        config.setEnvironment(Environment.TEST);
        config.setPosTerminalManagementApiEndpoint("https://postfmapi-test.adyen.com/postfmapi/terminal");

        Client APIClient = new Client(config);
        PosTerminalManagement managementAPIClient = new PosTerminalManagement(APIClient);


        GetTerminalsUnderAccountRequest request = new GetTerminalsUnderAccountRequest();
        request.setCompanyAccount(this.company_account);
        request.setMerchantAccount(this.merchant_account);

        try{
            Toast.makeText(getApplicationContext(), "Fetching available terminals under "+this.merchant_account, Toast.LENGTH_SHORT).show();
            GetTerminalsUnderAccountResponse response = managementAPIClient.getTerminalsUnderAccount(request);
            List<MerchantAccount> merchantAccountsList = response.getMerchantAccounts();
            List<Store> availableStores = new ArrayList<>();

            for(int i = 0 ; i< merchantAccountsList.size(); i ++ ){
                List<Store> store = merchantAccountsList.get(i).getStores();
                availableStores.addAll(store);
            }

            for (int j = 0 ; j < availableStores.size(); j ++){
                List<String> availableInStoreTerminals = availableStores.get(j).getInStoreTerminals();
                inStoreTerminals.addAll(availableInStoreTerminals);
            }
        }
        catch (Exception e){
            System.out.println("EXCEPTION!");
            Toast.makeText(getApplicationContext(), "Something went wrong when getting list of terminals", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return inStoreTerminals;
    }

    public void onClickSaveConfiguration(View view){
        Toast.makeText(getApplicationContext(), "Saving preferences", Toast.LENGTH_SHORT).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        //Saving Merchant Account
        EditText merchantAccount = (EditText) findViewById(R.id.MerchantAccountValue);
        editor.putString("merchant_account",merchantAccount.getText().toString());
        editor.apply();

        //Saving API Key
        EditText apiKey = (EditText) findViewById(R.id.APIKeyValue);
        editor.putString("api_key",apiKey.getText().toString());
        editor.apply();

        //Saving Cryto Value
        EditText crytoValue = (EditText) findViewById(R.id.crytoverisonvalue);
        editor.putString("crypto_version",crytoValue.getText().toString());
        editor.apply();

        //Saving local key_identifier
        EditText key_identifier = (EditText) findViewById(R.id.keyidentifiervalue);
        editor.putString("key_identifier",key_identifier.getText().toString());
        editor.apply();

        //Saving local passphrase value
        EditText passphrase = (EditText) findViewById(R.id.passphrase_value);
        editor.putString("key_phrase",passphrase.getText().toString());
        editor.apply();

        //saving key version value
        EditText key_version = (EditText) findViewById(R.id.key_version_value);
        editor.putString("key_version",key_version.getText().toString());
        editor.apply();

        //Saving Local IP Address
        EditText localIPField = (EditText) findViewById(R.id.LocalIPAddress);
        editor.putString("localIP",localIPField.getText().toString());
        editor.apply();

        //Saving Paired Terminal
        Spinner availableSpinner = (Spinner)findViewById(R.id.availableTerminalsDemo);
        if(availableSpinner.getSelectedItem() != null){
            String selectedPOI = availableSpinner.getSelectedItem().toString();
            editor.putString("pairedTerminal",selectedPOI);
            editor.apply();
        }

        Intent i = new Intent(DemoConfiguration.this, MainActivity.class);
        startActivity(i);
    }

    public void onClickFetchTerminal(View view){
        this.getTerminals();
        Spinner availableTerminals = findViewById(R.id.availableTerminalsDemo);
        List<String> terminals = this.getTerminals();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, terminals);
        //set the spinners adapter to the previously created one.
        availableTerminals.setAdapter(adapter);
    }
}