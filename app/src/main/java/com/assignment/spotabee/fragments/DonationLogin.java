package com.assignment.spotabee.fragments;

// Tutorial for PayPal Button: https://developer.paypal.com/docs/classic/mobile/ht_mpl-itemPayment-Android/


import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.assignment.spotabee.Config.Config;
import com.assignment.spotabee.R;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.assignment.spotabee.customutils.CheckNetworkConnection;
import com.assignment.spotabee.customutils.FileOp;
import com.assignment.spotabee.imagerecognition.ClarifaiClientGenerator;
import com.assignment.spotabee.imagerecognition.ClarifaiRequest;
import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.math.BigDecimal;

import clarifai2.api.ClarifaiClient;

import static android.app.Activity.RESULT_OK;
import static com.assignment.spotabee.Config.Config.PAYPAL_REQUEST_CODE;
import static com.assignment.spotabee.MainActivity.PICK_IMAGE;
import static com.assignment.spotabee.MainActivity.getContextOfApplication;
import com.assignment.spotabee.AmountPayed;


public class DonationLogin extends Fragment implements View.OnClickListener{
   private View rootView;
    private static final String API_KEY = "d984d2d494394104bb4bee0b8149523d";
    private static ClarifaiClient client;
    private static final String TAG = "Donation Login Debug";
    private CheckoutButton launchPayPalButton;
    private static final int PAYPAL_BUTTON_ID = 13098;
    private boolean _paypalLibraryInit;


    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)//Using Sandbox as it is a mock payment
            .clientId(Config.PAYPAL_CLIENT_ID);

    //Declare the buttons
    Button btnPayNow;
    EditText editAmount;

    String amount = "";

    //This method will help the app to destroy any information realated to the transaction when the user stop it
    protected void onDistroy() {
        getActivity().stopService(new Intent(getActivity(), PayPalService.class));
        super.onDestroy();
    }

    public DonationLogin() {
        // Required empty public constructor
    }

    public static DonationLogin newInstance(String param1, String param2) {
        DonationLogin fragment = new DonationLogin();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_donation, container, false);

        getActivity().setTitle(getString(R.string.donate));
        //Start Paypal Service
        Intent intent = new Intent(getActivity(), PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        getActivity().startService(intent);
        _paypalLibraryInit = false;


        //Declare the buttons
//        btnPayNow = (Button) rootView.findViewById(R.id.btnPayNow);
        editAmount = (EditText) rootView.findViewById(R.id.editAmount);

//        btnPayNow.setOnClickListener(new View.OnClickListener()
//
//        {
//            //This will set the payment process up once the payNow g
//            //Button is
//            public void onClick(View view) {
//                processPayment();
//            }
//        });

        initLibrary();
        showPayPalButton();

        return rootView;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case PAYPAL_BUTTON_ID:
                processPayment();
                break;
        }
    }

    private void processPayment() {

        amount = editAmount.getText().toString();
        AmountPayed.setAmountPayed(amount);
        try {
            PayPalPayment payPalPayment = new PayPalPayment(new BigDecimal(String.valueOf(amount)),
                    "USD", "Donate for Spot a Bee", PayPalPayment.PAYMENT_INTENT_SALE);
            Intent intent = new Intent(getActivity(), PaymentActivity.class);
            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
            getActivity().startActivityForResult(intent, PAYPAL_REQUEST_CODE);
        } catch (NumberFormatException e){
            Toast.makeText(getActivity(),
                    "Please enter numerical characters only. E.g, 10 for $10",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void showPayPalButton() {

        // Generate the PayPal checkout button and save it for later use
        PayPal pp = PayPal.getInstance();
        launchPayPalButton = pp.getCheckoutButton(getActivity(), PayPal.BUTTON_278x43, CheckoutButton.TEXT_PAY);

        // The OnClick listener for the checkout button
        launchPayPalButton.setOnClickListener(this);

        // Add the listener to the layout
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams (ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.bottomMargin = 10;
        launchPayPalButton.setLayoutParams(params);
        launchPayPalButton.setId(PAYPAL_BUTTON_ID);
        ((RelativeLayout) rootView.findViewById(R.id.payPalButtonContainer)).addView(launchPayPalButton);
//        ((RelativeLayout) rootView.findViewById(R.id.payPalButtonContainer)).setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public void initLibrary() {
        PayPal pp = PayPal.getInstance();

        if (pp == null) {  // Test to see if the library is already initialized

            // This main initialization call takes your Context, AppID, and target server
            pp = PayPal.initWithAppID(getActivity(), Config.PAYPAL_CLIENT_ID, PayPal.ENV_NONE);

            // Required settings:

            // Set the language for the library
            pp.setLanguage("en_US");

            // Some Optional settings:

            // Sets who pays any transaction fees. Value is:
            // FEEPAYER_SENDER, FEEPAYER_PRIMARYRECEIVER, FEEPAYER_EACHRECEIVER, and FEEPAYER_SECONDARYONLY
            pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);

            // true = transaction requires shipping
            pp.setShippingEnabled(true);

            _paypalLibraryInit = true;
        }
    }

}
