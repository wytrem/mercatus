package net.wytrem.spigot.mercatus.offers;

import net.wytrem.spigot.mercatus.Mercatus;
import net.wytrem.spigot.utils.WyPlugin;
import net.wytrem.spigot.utils.offers.OffersManager;
import net.wytrem.spigot.utils.offers.OffersSection;
import org.bukkit.entity.Player;

/**
 * Manages exchange offers.
 *
 * @see ExchangeOffer
 */
public class ExchangeOffersManager extends OffersManager<ExchangeOffer> {
    private Mercatus mercatus;

    public ExchangeOffersManager(WyPlugin plugin, OffersSection section) {
        super(plugin, section);
        this.mercatus = ((Mercatus) plugin);
    }

    @Override
    protected ExchangeOffer createDefaultOffer(Player sender, Player recipient) {
        return new ExchangeOffer(sender, recipient);
    }

    @Override
    protected void accept(ExchangeOffer offer) {
        if (Mercatus.instance.hasDistanceRestriction()) {
            Player sender = offer.getSender();
            Player recipient = offer.getRecipient();

            if (sender.getWorld() != recipient.getWorld()) {
                Mercatus.instance.texts.senderNotInSameWorld.format("offer", offer).send(recipient);
                return;
            }

            if (sender.getLocation().distance(recipient.getLocation()) > mercatus.getConf().exchanges.maxExchangeDistance) {
                Mercatus.instance.texts.senderTooFar.format("offer", offer).send(recipient);
                return;
            }
        }

        super.accept(offer);
    }

    @Override
    public void post(ExchangeOffer offer) {
        if (Mercatus.instance.hasDistanceRestriction()) {
            Player sender = offer.getSender();
            Player recipient = offer.getRecipient();

            if (sender.getWorld() != recipient.getWorld()) {
                Mercatus.instance.texts.recipientNotInSameWorld.format("offer", offer).send(sender);
                return;
            }

            if (sender.getLocation().distance(recipient.getLocation()) > mercatus.getConf().exchanges.maxExchangeDistance) {
                Mercatus.instance.texts.recipientTooFar.format("offer", offer).send(sender);
                return;
            }
        }
        super.post(offer);
    }
}
