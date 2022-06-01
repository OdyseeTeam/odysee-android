package com.odysee.app.callable;

import com.odysee.app.exceptions.ApiCallException;
import com.odysee.app.model.Transaction;
import com.odysee.app.utils.Lbry;

import java.util.List;
import java.util.concurrent.Callable;

public class TransactionList implements Callable<List<Transaction>> {
    private final int page;
    private final int pageSize;
    private final String authToken;

    public TransactionList(int page, int pageSize, String authToken) {
        this.page = page;
        this.pageSize = pageSize;
        this.authToken = authToken;
    }

    @Override
    public List<Transaction> call() throws Exception {
        List<Transaction> transactions = null;
        try {
            if (authToken != null)
                transactions = Lbry.transactionList(page, pageSize, authToken);
            else
                transactions = Lbry.transactionList(page, pageSize, "");
        } catch (ApiCallException ex) {
            ex.printStackTrace();
        }

        return transactions;
    }
}
