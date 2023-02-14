package com.odysee.app;

import androidx.appcompat.app.AppCompatActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.odysee.app.utils.Helper;

public class ClearDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_data);

        Context context = this;

        findViewById(R.id.clear_data_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // The "clear data" Android mechanism doesn't remove accounts, so Odysee has to do it
                AccountManager am = AccountManager.get(context);
                Account odyseeAccount = Helper.getOdyseeAccount(am.getAccounts());

                if (odyseeAccount != null) {
                    am.removeAccountExplicitly(odyseeAccount);
                }

                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
            }
        });
    }
}