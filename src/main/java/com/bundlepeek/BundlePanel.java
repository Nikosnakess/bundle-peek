package com.bundlepeek;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.BundleItemSelectedC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import com.bundlepeek.mixin.HandledScreenAccessor;

/**
 * Finestra del bundle: e' il suggerimento ORIGINALE del gioco (o della mod che lo modifica, come MetalBundles),
 * ma disegnato in una posizione fissa accanto allo slot, cosi' resta aperto finche' il mouse e' sullo slot
 * o dentro la finestra. Cliccando un oggetto si prende; clic destro lo divide; Shift+clic lo sposta.
 */
public final class BundlePanel {
    private static Slot openSlot;                          // slot del bundle con la finestra aperta
    private static PanelGeometry.Rect content;             // ultimo rettangolo del contenuto disegnato
    private static List<CellMapper.Cell> cells = List.of(); // caselle (indice + posizione) dell'ultimo disegno
    private static int lastSelected = -1;                  // ultimo oggetto selezionato passando il mouse
    // Un clic sulla finestra e' stato assorbito: va ignorato anche il RILASCIO del tasto (e il trascinamento).
    // Con il cursore pieno il gioco esegue un clic al rilascio, che piazzerebbe/butterebbe un oggetto.
    // Questo flag NON si azzera quando la finestra si chiude (succede subito, perche' il cursore si riempie).
    private static boolean swallowRelease;

    private static final FixedPositioner POSITIONER = new FixedPositioner();

    private BundlePanel() {}

    /** Usato dalla mixin: mentre la nostra finestra e' aperta il suggerimento normale non va disegnato. */
    public static boolean isActive() {
        return openSlot != null;
    }

    private static void reset() {
        openSlot = null;
        content = null;
        cells = List.of();
        lastSelected = -1;
    }

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            reset();
            swallowRelease = false;
            if (!(screen instanceof HandledScreen<?> handledScreen) || screen instanceof CreativeInventoryScreen) {
                return;
            }
            ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, delta) ->
                    update(client, handledScreen, mouseX, mouseY));
            ScreenEvents.afterRender(screen).register((s, context, mouseX, mouseY, delta) ->
                    render(client, handledScreen, context, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) ->
                    allowClick(client, handledScreen, click));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, click) -> {
                if (swallowRelease) {
                    swallowRelease = false;
                    return false;
                }
                return true;
            });
            ScreenMouseEvents.allowMouseDrag(screen).register((s, click, dx, dy) -> !swallowRelease);
            ScreenEvents.remove(screen).register(s -> {
                reset();
                swallowRelease = false;
            });
        });
    }

    // ------------------------------------------------------------------ posizionamento

    /** Dice al gioco dove disegnare il suggerimento e ricorda il rettangolo che ne risulta. */
    private static final class FixedPositioner implements TooltipPositioner {
        PanelGeometry.Rect cell;
        PanelGeometry.Rect result;

        @Override
        public Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height) {
            PanelGeometry.Rect r = PanelGeometry.placeContent(this.cell, width, height, screenWidth, screenHeight);
            this.result = r;
            return new Vector2i(r.x(), r.y());
        }
    }

    // ------------------------------------------------------------------ stato

    private static BundleContentsComponent contents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        BundleContentsComponent c = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
        return c != null && !c.isEmpty() ? c : null;
    }

    private static List<ItemStack> stacks(BundleContentsComponent c) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < c.size(); i++) {
            list.add(c.get(i));
        }
        return list;
    }

    private static PanelGeometry.Rect cellRect(HandledScreen<?> screen, Slot slot) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        return new PanelGeometry.Rect(accessor.bundlepeek$getX() + slot.x - 1,
                accessor.bundlepeek$getY() + slot.y - 1, PanelGeometry.CELL, PanelGeometry.CELL);
    }

    private static Slot slotAt(HandledScreen<?> screen, int mouseX, int mouseY) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.isEnabled() && cellRect(screen, slot).contains(mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /** Eseguito prima del disegno della schermata: decide se la finestra e' aperta in questo fotogramma. */
    private static void update(MinecraftClient client, HandledScreen<?> screen, int mouseX, int mouseY) {
        ScreenHandler handler = screen.getScreenHandler();
        // Con un oggetto in mano la finestra non si apre: coprirebbe gli slot dove vuoi posarlo.
        if (client.player == null || !handler.getCursorStack().isEmpty()) {
            reset();
            return;
        }
        if (openSlot != null) {
            if (contents(openSlot.getStack()) != null && handler.slots.contains(openSlot)
                    && PanelGeometry.keepOpen(cellRect(screen, openSlot), content, mouseX, mouseY)) {
                return; // il mouse e' ancora sullo slot o sulla finestra
            }
            reset();
        }
        Slot hovered = slotAt(screen, mouseX, mouseY);
        if (hovered != null && contents(hovered.getStack()) != null) {
            openSlot = hovered;
        }
    }

    // ------------------------------------------------------------------ disegno

    private static List<TooltipComponent> buildTooltip(MinecraftClient client, ItemStack stack) {
        List<TooltipComponent> list = new ArrayList<>();
        for (Text line : Screen.getTooltipFromItem(client, stack)) {
            list.add(TooltipComponent.of(line.asOrderedText()));
        }
        stack.getTooltipData().ifPresent(data -> list.add(Math.min(1, list.size()), TooltipComponent.of(data)));
        return list;
    }

    private static void render(MinecraftClient client, HandledScreen<?> screen, DrawContext context,
                               int mouseX, int mouseY) {
        if (openSlot == null) {
            return;
        }
        ItemStack bundle = openSlot.getStack();
        BundleContentsComponent c = contents(bundle);
        if (c == null) {
            return;
        }

        POSITIONER.cell = cellRect(screen, openSlot);
        POSITIONER.result = null;
        List<CellMapper.Captured<ItemStack>> captured;
        TooltipCapture.begin();
        try {
            // Lo stesso disegno del suggerimento originale, ma nella posizione scelta da noi.
            context.drawTooltipImmediately(client.textRenderer, buildTooltip(client, bundle),
                    mouseX, mouseY, POSITIONER, null);
        } finally {
            captured = TooltipCapture.end();
        }
        if (POSITIONER.result != null) {
            content = POSITIONER.result;
        }
        cells = CellMapper.map(captured, stacks(c), ItemStack::areEqual);

        // L'oggetto sotto il mouse diventa quello selezionato: il gioco lo evidenzia con la sua grafica.
        int hovered = CellMapper.cellAt(cells, mouseX, mouseY);
        if (hovered >= 0 && hovered != lastSelected) {
            selectIndex(client, screen, openSlot, hovered);
            lastSelected = hovered;
        }
    }

    // ------------------------------------------------------------------ clic

    private static boolean allowClick(MinecraftClient client, HandledScreen<?> screen, Click click) {
        swallowRelease = false; // un nuovo clic: l'eventuale rilascio precedente non conta piu'
        if (openSlot == null || content == null || client.player == null) {
            return true;
        }
        double mouseX = client.mouse.getScaledX(client.getWindow());
        double mouseY = client.mouse.getScaledY(client.getWindow());
        if (!PanelGeometry.hitArea(content).contains(mouseX, mouseY)) {
            return true; // clic sullo slot del bundle o altrove: comportamento normale (anche Mouse Tweaks)
        }

        // Clic dentro la finestra: non deve arrivare agli slot che stanno sotto.
        swallowRelease = true;
        int index = CellMapper.cellAt(cells, mouseX, mouseY);
        int button = click.button();
        BundleContentsComponent c = contents(openSlot.getStack());
        if (c != null && index >= 0 && index < c.size() && (button == 0 || button == 1)
                && canTake(client, openSlot)) {
            ItemStack target = c.get(index);
            if (button == 1) {
                split(client, screen, openSlot, index, target);
            } else if (client.isShiftPressed()) {
                quickMove(client, screen, openSlot, index, target);
            } else {
                take(client, screen, openSlot, index);
            }
        }
        return false;
    }

    /** Evita slot di sola uscita (risultato di banco, fornace...) dove il clic destro farebbe altro. */
    private static boolean canTake(MinecraftClient client, Slot slot) {
        return slot.canTakeItems(client.player) && slot.canInsert(slot.getStack());
    }

    private static void clickSlot(MinecraftClient client, HandledScreen<?> screen, Slot slot,
                                  int button, SlotActionType type) {
        client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId, slot.id, button, type, client.player);
    }

    /** Seleziona l'oggetto sia per il gioco sia per Item Interactions (se presente). */
    private static void selectIndex(MinecraftClient client, HandledScreen<?> screen, Slot slot, int index) {
        screen.getScreenHandler().selectBundleStack(slot.id, index);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new BundleItemSelectedC2SPacket(slot.id, index));
        }
        ItemInteractionsCompat.setCurrentSlot(client.player, index);
    }

    /** Clic sinistro: l'intera pila va sul cursore (selezione + clic destro "normale" sul bundle). */
    private static void take(MinecraftClient client, HandledScreen<?> screen, Slot bundleSlot, int index) {
        if (client.getNetworkHandler() == null || client.interactionManager == null) {
            return;
        }
        selectIndex(client, screen, bundleSlot, index);
        clickSlot(client, screen, bundleSlot, 1, SlotActionType.PICKUP);
    }

    /**
     * Clic destro: divide la pila. Il gioco non permette di togliere meta' pila da un bundle, quindi si usa uno
     * slot vuoto di appoggio: pila -> appoggio -> si prende meta' -> l'altra meta' torna nel bundle -> si prende il resto.
     * Senza pile da dividere o senza slot libero, prende la pila intera.
     */
    private static void split(MinecraftClient client, HandledScreen<?> screen, Slot bundleSlot, int index,
                              ItemStack target) {
        Slot scratch = target.getCount() >= 2 ? scratchSlot(screen, bundleSlot, target, false) : null;
        take(client, screen, bundleSlot, index);                          // 1) pila intera sul cursore
        if (scratch == null) {
            return;
        }
        clickSlot(client, screen, scratch, 0, SlotActionType.PICKUP);     // 2) pila intera nello slot di appoggio
        clickSlot(client, screen, scratch, 1, SlotActionType.PICKUP);     // 3) meta' (arrotondata per eccesso) sul cursore
        clickSlot(client, screen, bundleSlot, 1, SlotActionType.PICKUP);  // 4) quella meta' torna nel bundle
        clickSlot(client, screen, scratch, 0, SlotActionType.PICKUP);     // 5) l'altra meta' sul cursore
    }

    /**
     * Shift + clic sinistro: sposta la pila nell'altro inventario, come Shift+clic su uno slot normale.
     * Passa da uno slot vuoto di appoggio per usare le regole del gioco. Se non c'e' posto, prende la pila.
     */
    private static void quickMove(MinecraftClient client, HandledScreen<?> screen, Slot bundleSlot, int index,
                                  ItemStack target) {
        Slot scratch = scratchSlot(screen, bundleSlot, target, true);
        take(client, screen, bundleSlot, index);                          // 1) pila sul cursore
        if (scratch == null) {
            return;
        }
        clickSlot(client, screen, scratch, 0, SlotActionType.PICKUP);     // 2) pila nello slot di appoggio
        clickSlot(client, screen, scratch, 0, SlotActionType.QUICK_MOVE); // 3) Shift+clic del gioco
    }

    // ------------------------------------------------------------------ slot di appoggio

    private static int zone(Slot slot) {
        if (slot.inventory instanceof PlayerInventory) {
            int i = slot.getIndex();
            return i < 9 ? 0 : (i < 36 ? 1 : SlotChooser.EXCLUDED_ZONE);
        }
        if (slot.inventory instanceof RecipeInputInventory || slot.inventory instanceof CraftingResultInventory) {
            return SlotChooser.EXCLUDED_ZONE;
        }
        return 3;
    }

    private static Slot scratchSlot(HandledScreen<?> screen, Slot bundleSlot, ItemStack stack, boolean sameZoneOnly) {
        List<SlotChooser.SlotInfo> infos = new ArrayList<>();
        for (Slot s : screen.getScreenHandler().slots) {
            infos.add(new SlotChooser.SlotInfo(s.id, zone(s), !s.hasStack(), s.isEnabled(), s.canInsert(stack)));
        }
        int id = SlotChooser.choose(infos, bundleSlot.id, zone(bundleSlot), sameZoneOnly);
        if (id < 0) {
            return null;
        }
        for (Slot s : screen.getScreenHandler().slots) {
            if (s.id == id) {
                return s;
            }
        }
        return null;
    }
}
