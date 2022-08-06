package com.example.adyenpointofsale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.enums.Environment;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountRequest;
import com.adyen.model.posterminalmanagement.GetTerminalsUnderAccountResponse;
import com.adyen.model.posterminalmanagement.MerchantAccount;
import com.adyen.model.posterminalmanagement.Store;
import com.adyen.service.PosTerminalManagement;

import java.util.ArrayList;
import java.util.List;

public class DemoConfiguration extends AppCompatActivity {

    private String api_key=null;
    private String merchant_account=null;
    private String company_account = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.company_account = prefs.getString("company_account",null);
        this.merchant_account = prefs.getString("merchant_account",null);
        this.api_key = prefs.getString("api_key",null);
        setContentView(R.layout.activity_demo_configuration);
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

            GetTerminalsUnderAccountResponse response = managementAPIClient.getTerminalsUnderAccount(request);
            List<MerchantAccount> merchantAccountsList = response.getMerchantAccounts();
            List<Store> availableStores = new ArrayList<>();

            for(int i = 0 ; i< merchantAccountsList.size(); i ++ ){
                List<Store> store = merchantAccountsList.get(i).getStores();
                availableStores.addAll(store);
            }

            Log.i("available store", Integer.toString(availableStores.size()) );
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

    public void fetchTerminalsClick(View view){
        this.getTerminals();
        Spinner availableTerminals = findViewById(R.id.availableTerminals);
        List<String> terminals = this.getTerminals();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, terminals);
        //set the spinners adapter to the previously created one.
        availableTerminals.setAdapter(adapter);
    }
}