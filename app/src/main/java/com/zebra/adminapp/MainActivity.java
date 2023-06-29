package com.zebra.adminapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements EMDKManager.EMDKListener {

    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;
    EditText txtServiceIdentifier = null;
    EditText txtPackageName = null;
    // Provides the error type for characteristic-error
    private String errorType = "";
    // Provides the parm name for parm-error
    private String parmName = "";
    // Provides error description
    private String errorDescription = "";
    // Provides error string with type/name + description
    private String errorString = "";

    private String mToken = "";

    TextView txtStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtServiceIdentifier = findViewById(R.id.txtServiceIdentifier);
        txtPackageName = findViewById(R.id.txtPackageName);
        txtStatus = findViewById(R.id.txtStatus);

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        }else {
            //EMDKManager object creation failed
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onClosed() {

        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        //This callback will be issued when the EMDK is ready to use.
        updateStatus("EMDK open success.");

        this.emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

    }


    public void btnOnClickAllowCallerToCallService(View view)
    {

        updateStatus("Please wait...");
        profileName = "AllowCallerToCallService";
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "  <characteristic type=\"Profile\">\n" +
                "  <parm name=\"ProfileName\" value=\"AllowCallerToCallService\"/>" +
                "   <characteristic type=\"AccessMgr\" version=\"8.3\">\n" +
                "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                "      <parm name=\"OperationMode\" value=\"1\"/>\n" +
                "      <parm name=\"ServiceAccessAction\" value=\"4\"/>\n" +
                "      <parm name=\"ServiceIdentifier\" value=\""+txtServiceIdentifier.getText()+"\"/>\n" +
                "      <parm name=\"CallerPackageName\" value=\""+txtPackageName.getText()+"\"/>\n" +
                "      <parm name=\"CallerSignature\" value=\""+getCallerSignatureBase64Encoded(txtPackageName.getText().toString())+"\"/>\n" +
                "    </characteristic>" +
                "  </characteristic>";
        new ProcessProfileTask().execute(xml);

    }

    public void btnOnClickDisAllowCallerToCallService(View view)
    {

        updateStatus("Please wait...");
        profileName = "DisAllowCaller";
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "  <characteristic type=\"Profile\">\n" +
                "  <parm name=\"ProfileName\" value=\"DisAllowCaller\"/>" +
                "   <characteristic type=\"AccessMgr\" version=\"8.3\">\n" +
                "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                "      <parm name=\"OperationMode\" value=\"1\"/>\n" +
                "      <parm name=\"ServiceAccessAction\" value=\"5\"/>\n" +
                "      <parm name=\"ServiceIdentifier\" value=\""+txtServiceIdentifier.getText()+"\"/>\n" +
                "      <parm name=\"CallerPackageName\" value=\""+txtPackageName.getText()+"\"/>\n" +
                "      <parm name=\"CallerSignature\" value=\""+getCallerSignatureBase64Encoded(txtPackageName.getText().toString())+"\"/>\n" +
                "    </characteristic>" +
                "  </characteristic>";
        new ProcessProfileTask().execute(xml);

    }

    public void btnOnClickVerifyCallerToCallService(View view)
    {

        updateStatus("Please wait...");
        profileName = "VerifyCallerToCallService";
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "  <characteristic type=\"Profile\">\n" +
                "  <parm name=\"ProfileName\" value=\"VerifyCallerToCallService\"/>" +
                "   <characteristic type=\"AccessMgr\" version=\"8.3\">\n" +
                "      <parm name=\"emdk_name\" value=\"\"/>\n" +
                "      <parm name=\"OperationMode\" value=\"1\"/>\n" +
                "      <parm name=\"ServiceAccessAction\" value=\"6\"/>\n" +
                "      <parm name=\"ServiceIdentifier\" value=\""+txtServiceIdentifier.getText()+"\"/>\n" +
                "      <parm name=\"CallerPackageName\" value=\""+txtPackageName.getText()+"\"/>\n" +
                "      <parm name=\"CallerSignature\" value=\""+getCallerSignatureBase64Encoded(txtPackageName.getText().toString())+"\"/>\n" +
                "    </characteristic>" +
                "  </characteristic>";
        new ProcessProfileTask().execute(xml);
    }


    void updateStatus(final String status)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText("Status: " + status);
            }
        });
    }

    // Method to parse the XML response using XML Pull Parser
    public void parseXML(XmlPullParser myParser) {
        int event;
        try {
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = myParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:
                        if (name.equals("parm-error")) {
                            parmName = myParser.getAttributeValue(null, "name");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";
                            return;
                        }
                        else if (name.equals("parm")) {
                            String parmName = myParser.getAttributeValue(null, "name");
                            if(parmName.equalsIgnoreCase("ServiceAccessToken"))
                            {
                                mToken = myParser.getAttributeValue(null, "value");

                                return;
                            }

                        }
                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }
                event = myParser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private class ProcessProfileTask extends AsyncTask<String, Void, EMDKResults> {

        @Override
        protected EMDKResults doInBackground(String... params) {

            parmName = "";
            errorDescription = "";
            errorType = "";
            mToken = "";

            EMDKResults resultsReset = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.RESET, params);
            EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);

            return results;
        }

        @Override
        protected void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    parseXML(parser);

                    if (TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription) ) {

                        resultString = "Profile update success.";
                        if(!TextUtils.isEmpty(mToken))
                        {
                            resultString += "\nToken: " + mToken;
                            txtPackageName.setText(mToken);
                        }
                    }
                    else {

                        resultString = "Profile update failed." + errorString;
                    }

                } catch (XmlPullParserException e) {
                    resultString =  e.getMessage();
                }
            }

            updateStatus(resultString);
        }
    }

    String getCallerSignatureBase64Encoded(String packageName) {
        String callerSignature = null;

        try {
            Signature sig = getApplicationContext().getPackageManager().getPackageInfo(packageName,
                    64).signatures[0];
            if (sig != null) {
                byte[] data = Base64.encode(sig.toByteArray(), 0);
                String signature = new String(data, StandardCharsets.UTF_8);
                callerSignature = signature.replaceAll("\\s+", "");
                Log.d("SignatureVerifier", "caller signature:" + callerSignature);
            }
        } catch (Exception var6) {
            Log.e("SignatureVerifier", "exception in getting application signature:" + var6.toString());
        }

        return callerSignature;
    }
}
