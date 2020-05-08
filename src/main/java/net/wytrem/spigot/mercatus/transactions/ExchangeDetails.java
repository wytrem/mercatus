package net.wytrem.spigot.mercatus.transactions;

import net.wytrem.spigot.utils.transactions.TransactionDetails;

/**
 * An exchange transaction details. Empty for the moment.
 */
public class ExchangeDetails extends TransactionDetails {

    public static final ExchangeDetails INSTANCE = new ExchangeDetails();

    private ExchangeDetails() {}
}
