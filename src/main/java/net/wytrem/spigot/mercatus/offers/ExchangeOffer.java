package net.wytrem.spigot.mercatus.offers;

import net.wytrem.spigot.mercatus.Mercatus;
import net.wytrem.spigot.utils.offers.Offer;
import org.bukkit.entity.Player;

/**
 * An exchange offer. Calls {@link Mercatus#engageExchange(ExchangeOffer)} when accepted.
 */
public class ExchangeOffer extends Offer {
    public ExchangeOffer(Player sender, Player recipient) {
        super(sender, recipient);
    }

    @Override
    public void accepted() {
        Mercatus.instance.engageExchange(this);
    }
}

