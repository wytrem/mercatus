package net.wytrem.spigot.mercatus.config;

import net.wytrem.spigot.mercatus.Mercatus;
import net.wytrem.spigot.mercatus.config.pojo.ItemStackRef;
import net.wytrem.spigot.mercatus.config.pojo.VanillaItemStackRef;
import net.wytrem.spigot.utils.config.annotatedconfignode.AnnotatedConfigNode;
import net.wytrem.spigot.utils.config.annotatedconfignode.ConfigNode;
import org.bukkit.Material;

public class ScreensSection extends AnnotatedConfigNode {


    @ConfigNode(comments = "The appearance of the left exchange button when the exchange is not yet accepted.")
    public ItemStackRef leftNotAccepted = new VanillaItemStackRef(Mercatus.instance.materialBridge.getGrayWool());

    @ConfigNode(comments = "The appearance of the left exchange button when the exchange is accepted.")
    public ItemStackRef leftAccepted = new VanillaItemStackRef(Mercatus.instance.materialBridge.getGreenWool());

    @ConfigNode(comments = "The appearance of the left exchange button when disabled.")
    public ItemStackRef leftCantAccept = new VanillaItemStackRef(Mercatus.instance.materialBridge.getRedWool());

    @ConfigNode(comments = "The appearance of the right exchange button when the exchange is not yet accepted.")
    public ItemStackRef rightNotAccepted = new VanillaItemStackRef(Mercatus.instance.materialBridge.getGrayWool());

    @ConfigNode(comments = "The appearance of the right exchange button when the exchange is accepted.")
    public ItemStackRef rightAccepted = new VanillaItemStackRef(Mercatus.instance.materialBridge.getGreenWool());

    @ConfigNode(comments = "The appearance of the right exchange button when disabled.")
    public ItemStackRef rightCantAccept = new VanillaItemStackRef(Mercatus.instance.materialBridge.getRedWool());

    @ConfigNode(comments = "The appearance of the money button when there is some money involved.")
    public ItemStackRef withMoney = new VanillaItemStackRef(Material.NETHER_STAR);

    @ConfigNode(comments = "The appearance of the money button when there is no money involved.")
    public ItemStackRef noMoney = new VanillaItemStackRef(Mercatus.instance.materialBridge.getGrayDye());

    @ConfigNode(comments = "The appearance of the button indicating the side of the player.")
    public ItemStackRef yourSide = new VanillaItemStackRef(Material.ARROW);

    @ConfigNode(comments = "The appearance of the horizontal lines.")
    public ItemStackRef horizontalSeparator = new VanillaItemStackRef(Material.STICK);

    @ConfigNode(comments = "The appearance of the vertical lines.")
    public ItemStackRef verticalSeparator = new VanillaItemStackRef(Material.STICK);

    @ConfigNode(comments = "The appearance of the bottom left corner.")
    public ItemStackRef bottomLeftCorner = new VanillaItemStackRef(Material.STICK);

    @ConfigNode(comments = "The appearance of the bottom right corner.")
    public ItemStackRef bottomRightCorner = new VanillaItemStackRef(Material.STICK);

    @ConfigNode(comments = "The appearance of the top of the middle line.")
    public ItemStackRef middleTop = new VanillaItemStackRef(Material.STICK);

    @ConfigNode(comments = "The appearance of the bottom of the middle line.")
    public ItemStackRef middleBottom = new VanillaItemStackRef(Material.STICK);
}
