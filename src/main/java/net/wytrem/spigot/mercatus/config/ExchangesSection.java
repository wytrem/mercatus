package net.wytrem.spigot.mercatus.config;

import net.wytrem.spigot.utils.config.annotatedconfignode.AnnotatedConfigNode;
import net.wytrem.spigot.utils.config.annotatedconfignode.ConfigNode;

public class ExchangesSection extends AnnotatedConfigNode {
    @ConfigNode(comments = {"Set to true to enable money exchange. Needs Vault if true."})
    public boolean enableEconomy = true;

    @ConfigNode(comments = {"Prevent exchange between players who are too far from each other.", "Set to -1.0 to disable."})
    public double maxExchangeDistance = -1.0;
}
