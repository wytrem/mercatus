package net.wytrem.spigot.mercatus;

import net.wytrem.spigot.mercatus.config.MercatusConfig;
import net.wytrem.spigot.mercatus.config.pojo.VanillaItemStackRef;
import net.wytrem.spigot.mercatus.offers.ExchangeOffer;
import net.wytrem.spigot.mercatus.offers.ExchangeOffersManager;
import net.wytrem.spigot.mercatus.oraxen.OraxenDetecter;
import net.wytrem.spigot.mercatus.transactions.ExchangeDetails;
import net.wytrem.spigot.mercatus.transactions.Exchanges;
import net.wytrem.spigot.utils.WyPlugin;
import net.wytrem.spigot.utils.commands.Command;
import net.wytrem.spigot.utils.i18n.I18n;
import net.wytrem.spigot.utils.materialbridge.MaterialBridge;
import net.wytrem.spigot.utils.nms.NMSHelper;
import net.wytrem.spigot.utils.screens.Screens;
import net.wytrem.spigot.utils.text.Text;
import net.wytrem.spigot.utils.text.TextsRegistry;
import net.wytrem.spigot.utils.vault.VaultBridge;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.Arrays;

/**
 * Main Mercatus class. Registers commands and service.
 */
public class Mercatus extends WyPlugin<MercatusConfig> {
    // Instance
    public static Mercatus instance;

    // Services
    public Exchanges exchanges;
    public Texts texts;
    public Screens screens;
    public VaultBridge vault;
    public MaterialBridge materialBridge;

    // State
    public transient boolean enableEconomy;

    @Override
    protected void preConfigLoad() {
        super.preConfigLoad();

        this.nmsHelper.load(MaterialBridge.class);
        this.materialBridge = this.nmsHelper.get(MaterialBridge.class);
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        this.texts = new Texts(this.i18n);

        // Offers
        ExchangeOffersManager offersManager = new ExchangeOffersManager(this, this.config.offers);
        this.enableService(offersManager);

        // Commands
        Command exchange = this.commands.builder()
                .child(offersManager.buildAcceptCommand(), "accept")
                .child(offersManager.buildDenyCommand(), "deny")
                .child(offersManager.buildTakeBackCommand(), "takeback")
                .child(offersManager.buildProposeCommand(), "offer")
                .child(offersManager.buildListCommand(), "pending")
                .build();

        this.commands.register(exchange, "exchange");

        // Exchange transaction service
        this.exchanges = new Exchanges(this);
        this.enableService(this.exchanges);

        // Screens
        this.screens = new Screens(this);
        this.enableService(this.screens);

        // Load Vault if needed
        if (this.config.exchanges.enableEconomy) {
            this.enableEconomy = true;

            // Try to load Vault economy and sets enableEconomy to false if it fails
            if (VaultBridge.detectVault()) {
                // Enable VaultBridge
                this.vault = new VaultBridge(this);
                this.enableService(this.vault);

                // Enable economy
                if (!this.vault.setupEconomy()) {
                    getLogger().warning("Detected Vault but could not setup economy.");
                    this.enableEconomy = false;
                }
                else {
                }
            }
            else {
                getLogger().warning("Tried to enable economy but Vault was not detected on this server.");
                this.enableEconomy = false;
            }
        }
        else {
            getLogger().info("Not using Vault.");
        }
    }

    @Override
    protected void registerYamlTypes() {
        super.registerYamlTypes();
        ConfigurationSerialization.registerClass(VanillaItemStackRef.class);
        if (OraxenDetecter.detectOraxen()) {
            getLogger().info("Hooking with Oraxen.");
            OraxenDetecter.registerTypes();
        }
    }

    @Override
    protected Class<MercatusConfig> getConfigType() {
        return MercatusConfig.class;
    }

    /**
     * @return true if a player cannot accept an exchange from a player who is to far away.
     */
    public boolean hasDistanceRestriction() {
        return this.config.exchanges.maxExchangeDistance > 0.0;
    }

    @Override
    public String getCodeName() {
        return "mercatus";
    }

    /**
     * Engage an exchange transaction based on the given offer.
     */
    public void engageExchange(ExchangeOffer offer) {
        this.exchanges.initiate(ExchangeDetails.INSTANCE, Arrays.asList(offer.getSender(), offer.getRecipient()));
    }

    /**
     * Main I18n registry.
     */
    public static class Texts extends TextsRegistry {
        public Text exchange;
        public Text youDontHaveEnoughSpace;
        public Text youHaveNotAcceptedYet;
        public Text youAccepted;
        public Text otherHasNotEnoughSpace;
        public Text otherHaveNotAcceptedYet;
        public Text otherAccepted;
        public Text exchangeConfirmation;
        public Text exchangeCancelled;
        public Text recipientNotInSameWorld;
        public Text recipientTooFar;
        public Text senderNotInSameWorld;
        public Text senderTooFar;
        public Text yourSide;
        public Text moneyAmount;
        public Text addMoney;
        public Text enterAmount;
        public Text youHaveNotEnoughMoney;

        public Texts(I18n i18n) {
            super(i18n, "texts");
        }

        @Override
        public void load() {
            this.exchange = this.get("exchange");
            this.youDontHaveEnoughSpace = this.get("youDontHaveEnoughSpace").asError();
            this.youHaveNotAcceptedYet = this.get("youHaveNotAcceptedYet").asInformation();
            this.youAccepted = this.get("youAccepted").asInformation();
            this.otherHasNotEnoughSpace = this.get("otherHasNotEnoughSpace").asInformation();
            this.otherHaveNotAcceptedYet = this.get("otherHaveNotAcceptedYet").asInformation();
            this.otherAccepted = this.get("otherAccepted").asInformation();
            this.exchangeConfirmation = this.get("exchangeProceed").asSuccess();
            this.exchangeCancelled = this.get("exchangeCancelled").asInformation();
            this.recipientNotInSameWorld = this.get("recipientNotInSameWorld").asError();
            this.recipientTooFar = this.get("recipientTooFar").asError();
            this.senderNotInSameWorld = this.get("senderNotInSameWorld").asError();
            this.senderTooFar = this.get("senderTooFar").asError();
            this.yourSide = this.get("yourSide").asSuccess();
            this.moneyAmount = this.get("moneyAmount").asInformation();
            this.addMoney = this.get("addMoney").asInformation();
            this.enterAmount = this.get("enterAmount").asInformation();
            this.youHaveNotEnoughMoney = this.get("youHaveNotEnoughMoney").asInformation();
        }
    }
}
