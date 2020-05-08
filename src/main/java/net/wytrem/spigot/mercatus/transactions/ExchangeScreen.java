package net.wytrem.spigot.mercatus.transactions;

import com.google.common.base.Preconditions;
import net.milkbowl.vault.economy.Economy;
import net.wytrem.spigot.mercatus.Mercatus;
import net.wytrem.spigot.mercatus.config.ScreensSection;
import net.wytrem.spigot.utils.conversation.NumericPrompt;
import net.wytrem.spigot.utils.inventory.WyInventory;
import net.wytrem.spigot.utils.misc.ItemUtils;
import net.wytrem.spigot.utils.misc.math.Vec2i;
import net.wytrem.spigot.utils.screens.InventoryScreen;
import net.wytrem.spigot.utils.screens.Screens;
import net.wytrem.spigot.utils.text.Text;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The exchange screen.
 */
public class ExchangeScreen extends InventoryScreen {
    private Exchange.Side side;
    private ValidateButton validateButton;
    private Button otherValidateButton;
    private Exchange exchange;
    private Button selfCurrencyButton, otherCurrencyButton;
    private Mercatus.Texts texts;
    private ScreensSection config;

    private Economy economy;
    private boolean isInAmountModal;

    public ExchangeScreen(Screens service, Player player, Exchange.Side side, Exchange exchange) {
        super(service, player, WyInventory.basic(player, 5, Mercatus.instance.texts.exchange));
        this.exchange = exchange;
        this.side = side;

        this.texts = Mercatus.instance.texts;
        if (Mercatus.instance.enableEconomy) {
            this.economy = Mercatus.instance.vault.getEconomy();
        }
        this.config = Mercatus.instance.getConf().screens;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public void setSide(Exchange.Side side) {
        this.side = side;
    }

    @Override
    public void show() {
        super.show();
    }
    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if (this.player == event.getPlayer()) {
            if (!this.isInAmountModal) {
                this.exchange.cancelExchange();
            }
        }
    }

    /**
     * Prepares the exchange inventory: adds buttons and delimiters.
     */
    public void prepareEmptyFacade() {
        // Separators
        {
            // Top line
            this.inventory.horizontalLine(1, 0, 7, this.config.horizontalSeparator.get());
            this.inventory.setItem(4, 0, this.config.middleTop.get());

            // Bottom line
            this.inventory.horizontalLine(1, 4, 7, this.config.horizontalSeparator.get());
            this.inventory.setItem(0, 4, this.config.bottomLeftCorner.get());
            this.inventory.setItem(8, 4, this.config.bottomRightCorner.get());
            this.inventory.setItem(4, 4, this.config.middleBottom.get());

            // Vertical lines
            this.inventory.verticalLine(0, 1, 3, this.config.verticalSeparator.get());
            this.inventory.verticalLine(4, 1, 3, this.config.verticalSeparator.get());
            this.inventory.verticalLine(8, 1, 3, this.config.verticalSeparator.get());
        }

        // Your side slot
        {
            Vec2i pos = this.side == Exchange.Side.LEFT ? new Vec2i(0, 2) : new Vec2i(8, 2);
            ItemStack yourSide = this.config.yourSide.get().clone();
            ItemUtils.setDisplayName(yourSide, texts.yourSide.string(player));
            this.inventory.setItem(pos, yourSide);
        }

        // Self validating button
        {
            validateButton = new ValidateButton(Material.RED_WOOL);
            if (this.side == Exchange.Side.LEFT) {
                this.add(0, 0, validateButton);
            } else {
                this.add(8, 0, validateButton);
            }

            this.displaySelfInvalidate();
        }

        // Other validating button
        {
            otherValidateButton = new Button(Material.RED_WOOL);
            otherValidateButton.disable();

            if (this.side == Exchange.Side.LEFT) {
                this.add(8, 0, otherValidateButton);
            } else {
                this.add(0, 0, otherValidateButton);
            }

            this.displayOtherInvalidate();
        }


        // Currency buttons
        if (Mercatus.instance.enableEconomy) {
            this.selfCurrencyButton = new CurrencyButton();

            if (this.side == Exchange.Side.LEFT) {
                this.add(2, 4, selfCurrencyButton);
            } else {
                this.add(6, 4, selfCurrencyButton);
            }

            this.displaySelfMoney(0.0);

            this.otherCurrencyButton = new CurrencyButton();
            this.otherCurrencyButton.disable();

            if (this.side == Exchange.Side.LEFT) {
                this.add(6, 4, otherCurrencyButton);
            } else {
                this.add(2, 4, otherCurrencyButton);
            }

            this.displayOtherMoney(0.0);
        }
    }

    @Override
    public void clicked(InventoryClickEvent event) {
        // This checks button clicks...
        super.clicked(event);

        // ...and cancels the event if a button was clicked, so in that case we don't want to continue.
        if (event.isCancelled()) {
            return;
        }

        // Forwards relevant events to the Exchange object.
        if (this.inventory.wasClicked(event)) {
            if (isInactive(event.getSlot())) {
                event.setCancelled(true);
                return;
            } else {
                Vec2i clicked = this.inventory.slotToPos(event.getSlot());
                if (this.side.getArea().isInBounds(clicked)) {
                    this.exchange.onPlayerChangeAreaContent(this.side);
                } else {
                    event.setCancelled(true);
                    return;
                }
            }
        } else if (event.getClickedInventory() == event.getWhoClicked().getInventory()) {
            this.exchange.onPlayerClickSelfInventory(event, side);
        }
    }

    @EventHandler
    public void dragged(InventoryDragEvent event) {
        if (event.getWhoClicked() != this.player) {
            return;
        }

        // Allow only dragging in valid slots.
        for (int rawSlot : event.getRawSlots()) {
            if (this.inventory.isSameInv(this.player.getOpenInventory().getInventory(rawSlot))) {
                int convertedSlot = this.player.getOpenInventory().convertSlot(rawSlot);
                Vec2i pos = this.inventory.slotToPos(convertedSlot);
                if (!this.side.getArea().isInBounds(pos)) {
                    event.setCancelled(true);
                }
            }
        }

        if (!event.isCancelled()) {
            this.exchange.onPlayerChangeAreaContent(this.side);
        }
    }

    /**
     * @return whether the clicked slot is a filler.
     */
    public boolean isInactive(int slot) {
        Vec2i pos = this.inventory.slotToPos(slot);

        if (pos.x == 0 && pos.y >= 1) {
            return true;
        }

        if (pos.x == 8 && pos.y >= 1) {
            return true;
        }

        if (pos.y == 5) {
            return true;
        }

        if (pos.y == 0 && pos.x != 0 && pos.x != 8) {
            return true;
        }

        if (pos.x == 4) {
            return true;
        }
        return false;
    }

    /**
     * Copies the given area to the given side of this screen.
     */
    public void updateArea(Exchange.Side side, WyInventory area) {
        this.inventory.setRect(area, side.getOrigin(), side.getAreaSize());
    }

    // Display methods: called by the exchange when its state changes.

    // TODO: format texts with exchange data
    private Text formatWithOther(Text text) {
        return text.format("other", this.exchange.getPlayer(this.side.other()));
    }

    public void displaySelfNotEnoughSpace() {
        this.validateButton.setDisplay(this.side == Exchange.Side.LEFT ? this.config.leftCantAccept.get().clone() : this.config.rightCantAccept.get().clone());
        this.validateButton.setText(texts.youDontHaveEnoughSpace);
        this.validateButton.disable();
    }

    public void displayOtherNotEnoughSpace() {
        this.otherValidateButton.setDisplay(this.side == Exchange.Side.RIGHT ? this.config.leftCantAccept.get().clone() : this.config.rightCantAccept.get().clone());
        this.otherValidateButton.setText(formatWithOther(texts.otherHasNotEnoughSpace));
    }

    public void displaySelfEnoughSpace() {
        this.validateButton.enable();
        this.displaySelfInvalidate();
    }

    public void displayOtherEnoughSpace() {
        this.displayOtherInvalidate();
    }

    public void displaySelfValidate() {
        this.validateButton.setDisplay(this.side == Exchange.Side.LEFT ? this.config.leftAccepted.get().clone() : this.config.rightAccepted.get().clone());
        this.validateButton.setText(texts.youAccepted);
    }

    public void displaySelfInvalidate() {
        this.validateButton.setDisplay(this.side == Exchange.Side.LEFT ? this.config.leftNotAccepted.get().clone() : this.config.rightNotAccepted.get().clone());
        this.validateButton.setText(texts.youHaveNotAcceptedYet);
    }

    public void displayOtherValidate() {
        this.otherValidateButton.setDisplay(this.side == Exchange.Side.RIGHT ? this.config.leftAccepted.get().clone() : this.config.rightAccepted.get().clone());
        this.otherValidateButton.setText(formatWithOther(texts.otherAccepted));
    }

    public void displayOtherInvalidate() {
        this.otherValidateButton.setDisplay(this.side == Exchange.Side.RIGHT ? this.config.leftNotAccepted.get().clone() : this.config.rightNotAccepted.get().clone());
        this.otherValidateButton.setText(formatWithOther(texts.otherHaveNotAcceptedYet));
    }

    public void displayOtherMoney(double amount) {
        if (amount > 0) {
            this.otherCurrencyButton.setDisplay(this.config.withMoney.get().clone());
        }
        else {
            this.otherCurrencyButton.setDisplay(this.config.noMoney.get().clone());
        }

        this.otherCurrencyButton.setText(Mercatus.instance.texts.moneyAmount.format("amount", economy.format(amount)));
    }

    public void displaySelfMoney(double amount) {
        if (amount > 0) {
            this.selfCurrencyButton.setDisplay(this.config.withMoney.get().clone());
            this.selfCurrencyButton.setText(Mercatus.instance.texts.moneyAmount.format("amount", economy.format(amount)));
        }
        else {
            this.selfCurrencyButton.setDisplay(this.config.noMoney.get().clone());
            this.selfCurrencyButton.setText(texts.addMoney);
        }
    }

    protected class CurrencyButton extends Button {
        public CurrencyButton() {
            super(Material.NETHER_STAR);
        }

        @Override
        public void onClick() {

            Prompt prompt = new NumericPrompt() {
                @Override
                protected Prompt acceptValidatedInput(ConversationContext conversationContext, Number number) {
                    ExchangeScreen.this.show();
                    double amount = number.doubleValue() < 0 ? 0.0 : number.doubleValue();

                    exchange.setMoneyAmount(side, amount);
                    isInAmountModal = false;

                    return END_OF_CONVERSATION;
                }

                @Override
                protected Text getFailedValidationText(ConversationContext context, Number invalidInput) {
                    return texts.youHaveNotEnoughMoney;
                }

                @Override
                protected boolean isNumberValid(ConversationContext context, Number input) {
                    return input.doubleValue() <= 0.0 || checkBalance(input.doubleValue());
                }

                @Override
                public Text getText(ConversationContext conversationContext) {
                    return texts.enterAmount;
                }
            };

            Conversation conversation = new ConversationFactory(service.getPlugin())
                    .withFirstPrompt(prompt)
                    .withLocalEcho(false)
                    .buildConversation(player);

            ExchangeScreen.this.close();
            isInAmountModal = true;
            conversation.begin();
        }
    }

    protected class ValidateButton extends Button {
        public ValidateButton(Material material) {
            super(material);
        }

        @Override
        public void onClick() {
            exchange.toggleValidated(side);
        }
    }

    private boolean checkBalance(double amount) {
        Preconditions.checkArgument(amount >= 0);
        if (amount == 0.0) {
            return true;
        }
        else {
            return economy.has(player, amount);
        }
    }
}
