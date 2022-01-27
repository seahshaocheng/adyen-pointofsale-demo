package com.example.adyenpointofsale;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;

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
import com.adyen.model.terminal.TerminalAPIRequest;
import com.adyen.model.terminal.TerminalAPIResponse;
import com.adyen.model.terminal.security.SecurityKey;
import com.adyen.service.TerminalCloudAPI;
import com.adyen.service.TerminalLocalAPI;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class MainActivity extends AppCompatActivity {

    public int serviceIdNumber = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        //initiating certification
    }

    private SaleToPOIRequest generatePOIRequest() {
        String saleID = "AndroidOne";
        String serviceID = "TEST"+Integer.toString(this.serviceIdNumber);
        this.serviceIdNumber++;
        String POIID = "V400m-346981680";
        String transactionID = "Android1";

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
        amountsReq.setCurrency("SGD");
        amountsReq.setRequestedAmount( BigDecimal.valueOf(20.99) );
        paymentTransaction.setAmountsReq(amountsReq);
        paymentRequest.setPaymentTransaction(paymentTransaction);
        saleToPOIRequest.setPaymentRequest(paymentRequest);

        return saleToPOIRequest;
    }

    public void makeLocalPayment(View view) throws FileNotFoundException, CertificateException {
        Log.e("Info","Initiating Local Payment");
        Config config = new Config();
        config.setMerchantAccount("MarkSeahSG");
        config.setTerminalCertificate(getResources().openRawResource(R.raw.adyen_terminalfleet_test));
        config.setTerminalApiLocalEndpoint("https://10.0.0.3");

        SecurityKey securityKey = new SecurityKey();
        securityKey.setAdyenCryptoVersion(1);
        securityKey.setKeyIdentifier("AdyenMarkSeahLocal");
        securityKey.setPassphrase("56d64382");
        securityKey.setKeyVersion(1);

        Client terminalLocalAPIClient = new Client(config);
        terminalLocalAPIClient.setEnvironment(Environment.TEST,null);
        TerminalLocalAPI terminalLocalAPI = new TerminalLocalAPI(terminalLocalAPIClient);
        SaleToPOIRequest saleToPOIRequest =  generatePOIRequest();

        TerminalAPIRequest terminalApiRequest = new TerminalAPIRequest();
        terminalApiRequest.setSaleToPOIRequest(saleToPOIRequest);
        try {
            TerminalAPIResponse terminalAPIResponse = terminalLocalAPI.request(terminalApiRequest, securityKey);
            Log.i("Info","The result"+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult());
            Log.i("Info","The result"+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse());



            Log.i("Info:","Completed Local Connection!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void makePayment(View view) throws FileNotFoundException, CertificateException {
        Log.e("Info","Initiating Cloud Payment");

        Config config = new Config();
        config.setMerchantAccount("MarkSeahSG");
        config.setApiKey("AQEyhmfxKIvPbRJAw0m/n3Q5qf3VaY9UCJ1+XWZe9W27jmlZil1pdYlY+w+RwJkDxpQiwd8QwV1bDb7kfNy1WIxIIkxgBw==-8Qds9eBD9ImdlLEZrRw6UST7YHD9QjiPpoiDxFnVpck=-6nJ93.?6c^)9Krj%");

        Client terminalCloudAPIClient = new Client(config);
        terminalCloudAPIClient.setEnvironment(Environment.TEST,null);
        TerminalCloudAPI terminalCloudAPI = new TerminalCloudAPI(terminalCloudAPIClient);

        SaleToPOIRequest saleToPOIRequest =  generatePOIRequest();
        TerminalAPIRequest terminalApiRequest = new TerminalAPIRequest();
        terminalApiRequest.setSaleToPOIRequest(saleToPOIRequest);

        try {
            TerminalAPIResponse terminalAPIResponse = terminalCloudAPI.sync(terminalApiRequest);
            Log.i("Info","The resultCode: "+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getResult());
            Log.i("Info","The additionalData"+terminalAPIResponse.getSaleToPOIResponse().getPaymentResponse().getResponse().getAdditionalResponse());
            Log.i("Info:","Completed Cloud Connection!");
        } catch (Exception e) {
            System.out.println("EXCEPTION!");
            e.printStackTrace();
        }
    }
}