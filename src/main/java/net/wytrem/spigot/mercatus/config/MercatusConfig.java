package net.wytrem.spigot.mercatus.config;

import net.wytrem.spigot.utils.config.WyPluginConfig;
import net.wytrem.spigot.utils.config.annotatedconfignode.ConfigNode;
import net.wytrem.spigot.utils.offers.OffersSection;

public class MercatusConfig extends WyPluginConfig {
    @ConfigNode(comments = {
            "--Offers options--",
            "This section holds the configuration for the offer system."
    })
    public OffersSection offers = new OffersSection();

    @ConfigNode(comments = {
            "--Exchanges options--",
            "This section holds the configuration for the exchange logic.",
            "For configuring the GUI, see the 'screens' section."
    })
    public ExchangesSection exchanges = new ExchangesSection();


    @ConfigNode(comments = {
            "--Screens (GUI) options--",
            "This section holds the configuration for the exchange GUI, excluding texts.",
            "Texts are managed through the i18n folder."
    })
    public ScreensSection screens = new ScreensSection();
}
