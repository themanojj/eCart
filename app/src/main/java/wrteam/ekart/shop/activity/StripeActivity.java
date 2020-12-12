package wrteam.ekart.shop.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentIntentResult;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardInputWidget;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import wrteam.ekart.shop.R;
import wrteam.ekart.shop.fragment.AddressListFragment;
import wrteam.ekart.shop.helper.ApiConfig;
import wrteam.ekart.shop.helper.Constant;
import wrteam.ekart.shop.helper.Session;
import wrteam.ekart.shop.helper.VolleyCallback;

public class StripeActivity extends AppCompatActivity {

    boolean isTxnInProcess = true;
    private Stripe stripe;
    Button payButton;
    Map<String, String> sendparams;
    Session session;
    Toolbar toolbar;
    TextView tvTitle, tvPayableAmount;
    private String paymentIntentClientSecret, stripePublishableKey, orderId, from;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stripe_payment);

        session = new Session(StripeActivity.this);
        sendparams = (Map<String, String>) getIntent().getSerializableExtra(Constant.PARAMS);
        orderId = getIntent().getStringExtra(Constant.ORDER_ID);
        from = getIntent().getStringExtra(Constant.FROM);

        toolbar = findViewById(R.id.toolbar);
        payButton = findViewById(R.id.payButton);
        tvTitle = findViewById(R.id.tvTitle);
        tvPayableAmount = findViewById(R.id.tvPayableAmount);

        if (from.equals(Constant.PAYMENT)) {
            tvTitle.setText(getString(R.string.app_name) + getString(R.string.shopping));
        } else {
            tvTitle.setText(getString(R.string.app_name) + getString(R.string.wallet_recharge_));
        }
        tvPayableAmount.setText(Constant.SETTING_CURRENCY_SYMBOL + (int) Math.round(Double.parseDouble(getIntent().getStringExtra(Constant.AMOUNT))));

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.stripe));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startCheckout();
    }


    private void startCheckout() {
        int pincode = 0;
        String[] address = new String[7];

        if (from.equals(Constant.PAYMENT)) {
            address = AddressListFragment.selectedAddress.split(", ");
        } else if (from.equals(Constant.WALLET)) {
            address = Constant.DefaultAddress.split(", ");
        }
        pincode = Integer.parseInt(address[6].replace(getString(R.string.pincode_), ""));

        Map<String, String> params = new HashMap<String, String>();
        params.put(Constant.NAME, session.getData(Constant.NAME));
        params.put(Constant.ADDRESS_LINE1, AddressListFragment.selectedAddress);
        params.put(Constant.POSTAL_CODE, "" + (int) pincode / 10);
        params.put(Constant.CITY, address[2]);
        params.put(Constant.AMOUNT, "" + (int) Math.round(Double.parseDouble(getIntent().getStringExtra(Constant.AMOUNT))));
        params.put(Constant.ORDER_ID, orderId);
        ApiConfig.RequestToVolley(new VolleyCallback() {
            @Override
            public void onSuccess(boolean result, String response) {
                if (result) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        stripePublishableKey = jsonObject.getString(Constant.publishableKey);
                        paymentIntentClientSecret = jsonObject.getString(Constant.clientSecret);

                        stripe = new Stripe(
                                getApplicationContext(),
                                Objects.requireNonNull(stripePublishableKey)
                        );

                        payButton.setOnClickListener((View view) -> {
                            CardInputWidget cardInputWidget = findViewById(R.id.cardInputWidget);
                            PaymentMethodCreateParams params = cardInputWidget.getPaymentMethodCreateParams();
                            if (params != null) {
                                ConfirmPaymentIntentParams confirmParams = ConfirmPaymentIntentParams
                                        .createWithPaymentMethodCreateParams(params, paymentIntentClientSecret);
                                stripe.confirmPayment(StripeActivity.this, confirmParams);
                            }
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, StripeActivity.this, Constant.STRIPE_BASE_URL, params, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isTxnInProcess = false;
        stripe.onPaymentResult(requestCode, data, new PaymentResultCallback(this));
    }

    public void AddTransaction(Activity activity, String orderId, String paymentType, String txnid, final String status, String message, Map<String, String> sendparams) {
        Map<String, String> transparams = new HashMap<>();
        transparams.put(Constant.ADD_TRANSACTION, Constant.GetVal);
        transparams.put(Constant.USER_ID, sendparams.get(Constant.USER_ID));
        transparams.put(Constant.ORDER_ID, orderId);
        transparams.put(Constant.TYPE, paymentType);
        transparams.put(Constant.TRANS_ID, txnid);
        transparams.put(Constant.AMOUNT, sendparams.get(Constant.FINAL_TOTAL));
        transparams.put(Constant.STATUS, status);
        transparams.put(Constant.MESSAGE, message);
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        transparams.put("transaction_date", df.format(c));
        ApiConfig.RequestToVolley(new VolleyCallback() {
            @Override
            public void onSuccess(boolean result, String response) {
                if (result) {
                    try {
                        JSONObject objectbject = new JSONObject(response);
                        if (!objectbject.getBoolean(Constant.ERROR)) {

                            if (from.equals(Constant.WALLET)) {
                                onBackPressed();
                                Toast.makeText(activity, "You amount will be credited in wallet very soon.", Toast.LENGTH_SHORT).show();
                            } else if (from.equals(Constant.PAYMENT)) {
                                if (status.equals(Constant.SUCCESS) || status.equals(Constant.AWAITING_PAYMENT)) {
                                    finish();
                                    Intent intent = new Intent(activity, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(Constant.FROM, "payment_success");
                                    activity.startActivity(intent);
                                } else {
                                    finish();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, activity, Constant.ORDERPROCESS_URL, transparams, true);
    }

    @Override
    public void onBackPressed() {
        if (isTxnInProcess)
            ProcessAlertDialog();
        else
            super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    public void ProcessAlertDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(StripeActivity.this);
        // Setting Dialog Message
        alertDialog.setMessage(getString(R.string.txn_cancel_msg));
        alertDialog.setCancelable(false);
        final AlertDialog alertDialog1 = alertDialog.create();
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DeleteTransaction(StripeActivity.this, getIntent().getStringExtra(Constant.ORDER_ID));
                onBackPressed();
                alertDialog1.dismiss();
            }
        }).setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog1.dismiss();
            }
        });
        // Showing Alert Message
        alertDialog.show();
    }

    public void DeleteTransaction(Activity activity, String orderId) {
        Map<String, String> transparams = new HashMap<>();
        transparams.put(Constant.DELETE_ORDER, Constant.GetVal);
        transparams.put(Constant.ORDER_ID, orderId);
        ApiConfig.RequestToVolley(new VolleyCallback() {
            @Override
            public void onSuccess(boolean result, String response) {
                if (result) {
                    onBackPressed();
                }
            }
        }, activity, Constant.ORDERPROCESS_URL, transparams, false);
    }

    private final class PaymentResultCallback implements ApiResultCallback<PaymentIntentResult> {

        PaymentResultCallback(@NonNull StripeActivity activity) {
            WeakReference<StripeActivity> activityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(@NonNull PaymentIntentResult result) {
            PaymentIntent paymentIntent = result.getIntent();
            PaymentIntent.Status status = paymentIntent.getStatus();
            if (status == PaymentIntent.Status.Succeeded) {
                AddTransaction(StripeActivity.this, orderId, getString(R.string.stripe), orderId, Constant.SUCCESS, "", sendparams);
            } else if (status == PaymentIntent.Status.Processing) {
                AddTransaction(StripeActivity.this, orderId, getString(R.string.stripe), orderId, Constant.AWAITING_PAYMENT, "", sendparams);
            }
            Toast.makeText(StripeActivity.this, getString(R.string.order_placed1), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(@NonNull Exception e) {
            DeleteTransaction(StripeActivity.this, orderId);
            Toast.makeText(StripeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}