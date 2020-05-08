package net.wytrem.spigot.mercatus.transactions;

import com.google.common.base.Preconditions;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.wytrem.spigot.mercatus.Mercatus;
import net.wytrem.spigot.utils.inventory.WyInventory;
import net.wytrem.spigot.utils.misc.math.Area;
import net.wytrem.spigot.utils.misc.math.Vec2i;
import net.wytrem.spigot.utils.text.Text;
import net.wytrem.spigot.utils.transactions.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * Main exchange class that performs inventory operations and screens updates.
 */
public class Exchange extends Transaction<ExchangeDetails, Exchange, Exchanges> implements Listener {
    protected Player left, right;
    protected ExchangeScreen leftScreen, rightScreen;
    protected WyInventory leftArea, rightArea;
    protected State state;

    public Exchange(Exchanges service, ExchangeDetails details) {
        super(service, details);
        this.state = new State();
    }

    /**
     * Initiates this exchange: creates inventories and open screens.
     *
     * @param players the players involved in this transaction. Size must be 2.
     */
    @Override
    public void initiate(Collection<Player> players) {
        Preconditions.checkArgument(players.size() == 2);
        super.initiate(players);
        Iterator<Player> playerIterator = players.iterator();
        this.left = playerIterator.next();
        this.right = playerIterator.next();
        this.leftScreen = new ExchangeScreen(Mercatus.instance.screens, this.left, Side.LEFT, this);
        this.rightScreen = new ExchangeScreen(Mercatus.instance.screens, this.right, Side.RIGHT, this);
        this.leftScreen.show();
        this.leftScreen.prepareEmptyFacade();
        this.rightScreen.show();
        this.rightScreen.prepareEmptyFacade();

        // Fake dispenser to simulate the 3x3 areas: allows the usage of convenient Iventory methods.
        this.leftArea = WyInventory.dispenser(null, Text.of("internal"));
        this.rightArea = WyInventory.dispenser(null, Text.of("internal"));
    }

    /**
     * Called from the screen when a player clicks on this own inventory (lower part in inventory view).
     * Sends the clicked stack to the exchange inventory if relevant, and broadcast the change on the other screen.
     */
    public void onPlayerClickSelfInventory(InventoryClickEvent event, Side side) {
        if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
            ItemStack stack = event.getWhoClicked().getInventory().getItem(event.getSlot());
            if (stack != null) {
                if (canStore(this.getArea(side), Arrays.asList(stack))) {
                    Player player = ((Player) event.getWhoClicked());
                    player.getInventory().setItem(event.getSlot(), null);
                    this.later(() -> {
                        this.getArea(side).addItem(stack);
                        this.copyAreaToInventoryScreens(side);
                        this.onAreaUpdated(side);
                    });
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Called from the exchange screen when a player updates its own inventory area.
     */
    public void onPlayerChangeAreaContent(Side side) {
        this.later(() -> {
            this.copyFromInventoryScreen(side);
            this.copyAreaToInventoryScreen(side, side.other());
            this.onAreaUpdated(side);
        });
    }

    /**
     * Called when the exchange data change. Update the state.
     */
    public void onAreaUpdated(Side side) {
        this.invalidate();
        this.updateCanValidate();
    }

    /**
     * Copies the area inside the screen of the given side to the exchange inventory.
     */
    protected void copyFromInventoryScreen(Side side) {
        this.getArea(side).setRect(this.getScreen(side).getInventory(), side.getOrigin(), Vec2i.ZERO, AREA_SIZE);
    }

    /**
     * Copy the content of the exchange inventory to the screens.
     *
     * @param side the screen to update
     */
    protected void copyAreaToInventoryScreens(Side side) {
        this.copyAreaToInventoryScreen(side, Side.LEFT);
        this.copyAreaToInventoryScreen(side, Side.RIGHT);
    }

    /**
     * Updates the given side in the given screen.
     */
    protected void copyAreaToInventoryScreen(Side side, Side screenToCopy) {
        this.getScreen(screenToCopy).updateArea(side, this.getArea(side));
    }

    /**
     * Delays the given runnable to the next tick.
     */
    protected void later(Runnable run) {
        Mercatus.instance.nextTick(run);
    }

    /**
     * Prints the exchange area to the console.
     */
    public void debug() {
        System.out.println("this.state = " + this.state);
        System.out.println("Left area content: " + this.getArea(Side.LEFT).getAll());
        System.out.println("Right area content: " + this.getArea(Side.RIGHT).getAll());
    }

    /**
     * Checks if both player can accept the exchange and updates the state consequently.
     */
    public void updateCanValidate() {
        Side.forEach(side -> {
            if (this.canValidate(side)) {
                this.state.setValidationState(side, SideState.NOT_YET_VALIDATED);
                this.getScreen(side).displaySelfEnoughSpace();
                this.getScreen(side.other()).displayOtherEnoughSpace();
            } else {
                this.state.setValidationState(side, SideState.NOT_ENOUGH_SPACE);
                this.getScreen(side).displaySelfNotEnoughSpace();
                this.getScreen(side.other()).displayOtherNotEnoughSpace();
            }
        });
    }

    /**
     * Called when the given side wishes to validate the exchange.
     */
    public void validate(Side side) {
        // Just double check
        Preconditions.checkArgument(canValidate(side));

        this.state.setValidationState(side, SideState.VALIDATED);

        this.getScreen(side).displaySelfValidate();
        this.getScreen(side.other()).displayOtherValidate();

        if (this.hasValidated(side.other())) {
            this.later(this::processExchange);
        }
    }

    /**
     * Give back stored stacks to their respective owners, send the exchange cancelled text and terminates this transaction.
     */
    public void cancelExchange() {
        Side.forEach(side -> {
            Player player = this.getPlayer(side);
            for (ItemStack stack : this.getArea(side).getAll()) {
                if (!player.getInventory().addItem(stack).isEmpty()) {
                    throw new IllegalStateException();
                }
            }
        });
        Mercatus.instance.texts.exchangeCancelled.send(this.left, this.right);

        // End transaction
        this.terminate();
    }

    /**
     * Gives the exchange content to the players and terminates the transaction.
     */
    public void processExchange() {
        if (Mercatus.instance.enableEconomy) {
            Economy economy = Mercatus.instance.vault.getEconomy();
            for (Side side : Side.values()) {
                Player player = this.getPlayer(side);

                double amount = this.state.getAmount(side.other());

                if (amount > 0) {
                    EconomyResponse response = economy.depositPlayer(player, amount);
                    if (!response.transactionSuccess()) {
                        this.terminate();
                        throw new RuntimeException("Could not depositPlayer(" + player.getName() + ", " + amount + ").");
                    }
                }

                amount = this.state.getAmount(side);

                if (amount > 0) {
                    EconomyResponse response = economy.withdrawPlayer(player, amount);
                    if (!response.transactionSuccess()) {
                        this.terminate();
                        throw new RuntimeException("Could not withdrawPlayer(" + player.getName() + ", " + amount + ").");
                    }
                }
            }
        }

        // Add the other side to each player
        Side.forEach(side -> {
            Player player = this.getPlayer(side);
            for (ItemStack stack : this.getArea(side.other()).getAll()) {
                if (!player.getInventory().addItem(stack).isEmpty()) {
                    throw new IllegalStateException();
                }
            }
        });

        Mercatus.instance.texts.exchangeConfirmation.send(this.left, this.right);

        // End transaction
        this.terminate();
    }

    /**
     * Called when the given side changes the amount of money.
     */
    public void setMoneyAmount(Side side, double amount) {
        // Just double-check
        Preconditions.checkArgument(amount >= 0);
        this.invalidate();
        this.state.setAmount(side, amount);
        this.getScreen(side).displaySelfMoney(amount);
        this.getScreen(side.other()).displayOtherMoney(amount);
    }


    @Override
    public void terminate() {
        super.terminate();
        this.leftScreen.close();
        this.rightScreen.close();
    }

    /**
     * Toggles the validated state for the given side and notifies the other side to update the display.
     */
    protected void toggleValidated(Side side) {
        if (this.hasValidated(side)) {
            this.invalidate(side);
        } else {
            this.validate(side);
        }
    }

    /**
     * Invalidate both side.
     */
    protected void invalidate() {
        this.invalidate(Side.LEFT);
        this.invalidate(Side.RIGHT);
    }

    /**
     * Invalidates the given side.
     */
    protected void invalidate(Side side) {
        this.state.setValidationState(side, SideState.NOT_YET_VALIDATED);

        this.getScreen(side).displaySelfInvalidate();
        this.getScreen(side.other()).displayOtherInvalidate();
    }

    /**
     * @return true if the given side validated
     */
    public boolean hasValidated(Side side) {
        return this.state.getValidationState(side) == SideState.VALIDATED;
    }

    /**
     * @return the screen for the player of the given side
     */
    public ExchangeScreen getScreen(Side side) {
        return side == Side.LEFT ? this.leftScreen : this.rightScreen;
    }

    /**
     * @return the player displayed on the given side
     */
    public Player getPlayer(Side side) {
        return side == Side.LEFT ? this.left : this.right;
    }

    /**
     * @return the player displayed on the given side
     */
    public WyInventory getArea(Side side) {
        return side == Side.LEFT ? this.leftArea : this.rightArea;
    }

    /**
     * @return true if the given side can be validated (i.e. if the player has enough space inside his inventory)
     */
    public boolean canValidate(Side side) {
        return canStore(this.getPlayer(side), this.getArea(side.other()).getAll());
    }

    /**
     * @return true if the given player can store the given items
     */
    public static boolean canStore(Player player, Iterable<ItemStack> items) {
        Inventory playerBase = Bukkit.createInventory(null, 36);
        for (int i = 0; i < playerBase.getSize(); i++) {
            playerBase.setItem(i, player.getInventory().getItem(i));
        }

        // TODO: fix this method (lookup in armor slots)

        return canStore(playerBase, items);
    }

    /**
     * @return true if the given inventory can store the given items
     */
    public static boolean canStore(WyInventory inv, Iterable<ItemStack> items) {
        return canStore(inv.getHandle(), items);
    }

    /**
     * @return true if the given inventory can store the given items
     */
    public static boolean canStore(Inventory inventory, Iterable<ItemStack> items) {
        Preconditions.checkArgument(inventory.getSize() % 9 == 0);
        Preconditions.checkArgument(inventory.getSize() <= 54);
        ItemStack[] invRef = inventory.getStorageContents();
        Inventory inv = Bukkit.createInventory(null, inventory.getSize(), "canStore");
        inv.setContents(invRef);
        inv.setMaxStackSize(inventory.getMaxStackSize());

        //For every item to be added to the player's inventory.
        for (ItemStack j : items) {
            if (j != null) {
                HashMap<Integer, ItemStack> extra = inv.addItem(j);
                if (!extra.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- GUI constants ---

    private static final Vec2i LEFT_AREA_ORIGIN = new Vec2i(1, 1);
    private static final Vec2i RIGHT_AREA_ORIGIN = new Vec2i(5, 1);
    private static final Vec2i AREA_SIZE = new Vec2i(3, 3);
    private static final Area LEFT_AREA = Area.fromSize(LEFT_AREA_ORIGIN, AREA_SIZE);
    private static final Area RIGHT_AREA = Area.fromSize(RIGHT_AREA_ORIGIN, AREA_SIZE);

    public enum Side {
        LEFT, RIGHT;

        public Vec2i getOrigin() {
            return this == LEFT ? LEFT_AREA_ORIGIN : RIGHT_AREA_ORIGIN;
        }

        public Area getArea() {
            return this == LEFT ? LEFT_AREA : RIGHT_AREA;
        }

        public Vec2i getAreaSize() {
            return AREA_SIZE;
        }

        public Side other() {
            return this == LEFT ? RIGHT : LEFT;
        }

        public static void forEach(Consumer<Side> consumer) {
            consumer.accept(LEFT);
            consumer.accept(RIGHT);
        }
    }

    /**
     * Represents the exchange state (maps a {@link SideState} to each {@link Side}).
     */
    class State {
        private Map<Side, SideState> sideStateMap;
        private Map<Side, Double> amounts;

        public State() {
            this.sideStateMap = new HashMap<>();
            this.sideStateMap.put(Side.LEFT, SideState.NOT_YET_VALIDATED);
            this.sideStateMap.put(Side.RIGHT, SideState.NOT_YET_VALIDATED);

            this.amounts = new HashMap<>();
            this.amounts.put(Side.LEFT, 0.0);
            this.amounts.put(Side.RIGHT, 0.0);
        }

        public SideState getValidationState(Side side) {
            return this.sideStateMap.get(side);
        }

        public void setValidationState(Side side, SideState state) {
            this.sideStateMap.put(side, state);
        }

        public double getAmount(Side side) {
            return amounts.get(side);
        }

        public void setAmount(Side side, double value) {
            this.amounts.put(side, value);
        }
    }

    enum SideState {
        NOT_ENOUGH_SPACE,
        NOT_YET_VALIDATED,
        VALIDATED;
    }
}
