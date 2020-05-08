package net.wytrem.spigot.mercatus.transactions;

import net.wytrem.spigot.utils.WyPlugin;
import net.wytrem.spigot.utils.transactions.Transactions;

/**
 * The main transaction manager.
 *
 * @see Exchange
 */
public class Exchanges extends Transactions<ExchangeDetails, Exchange> {
    public Exchanges(WyPlugin plugin) {
        super(plugin);
    }

    @Override
    protected Exchange create(ExchangeDetails exchangeDetails) {
        return new Exchange(this, exchangeDetails);
    }

    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        this.getOngoingTransactions().forEach(Exchange::cancelExchange);
    }
}
