
package com.example.adyenpointofsale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
    private String POS_ID = null;
    private String currency = null;
    private String reference_prefix = null;
    private String merchant_account=null;
    private String company_account = null;
    private String local_crypto_version=null;
    private String local_key_version=null;
    private String local_key_identifier=null;
    private String local_key_phrase=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Settings");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.POS_ID = prefs.getString("pos_id",null);
        this.currency = prefs.getString("tender_currency",null);
        this.reference_prefix = prefs.getString("reference_prefix",null);
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

        //POS ID
        EditText posID = (EditText) findViewById(R.id.POSIdValue);
        posID.setText(this.POS_ID);

        //currency
        EditText currencyField = (EditText) findViewById(R.id.currencyValue);
        currencyField.setText(this.currency);

        //reference prefix
        EditText referencePrefix = (EditText) findViewById(R.id.referencePrefixValue);
        referencePrefix.setText(this.reference_prefix);

        //Company Account
        EditText companyAccount = (EditText) findViewById(R.id.companyAccountValue);
        companyAccount.setText(this.company_account);

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

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.mymenu is a reference to an xml file named mymenu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.settingsmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.backdemo) {
            // do something here
            Intent i = new Intent(DemoConfiguration.this, MainActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
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

        //Saving POS ID
        EditText pos_id = (EditText) findViewById(R.id.POSIdValue);
        editor.putString("pos_id",pos_id.getText().toString());
        editor.apply();

        //Saving Currency
        EditText tender_currency = (EditText) findViewById(R.id.currencyValue);
        editor.putString("tender_currency",tender_currency.getText().toString());
        editor.apply();

        //Saving reference prefix
        EditText reference = (EditText) findViewById(R.id.referencePrefixValue);
        editor.putString("reference_prefix",reference.getText().toString());
        editor.apply();

        //Saving Company Account
        EditText companyAccount = (EditText) findViewById(R.id.companyAccountValue);
        editor.putString("company_account",companyAccount.getText().toString());
        editor.apply();

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

    public void scanAcccountConfiguration(View view){
        Intent i = new Intent(DemoConfiguration.this, ScannerActivity.class);
        i.putExtra("source","configuration");
        startActivity(i);
    }
}