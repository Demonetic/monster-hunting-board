package se.edugrade.monsterhuntingboard.service;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.InventoryItemResponse;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemRequest;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopResponse;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.HunterInventoryItem;
import se.edugrade.monsterhuntingboard.model.InventoryItemType;
import se.edugrade.monsterhuntingboard.repository.HunterInventoryItemRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
@RequiredArgsConstructor
public class ShopService {
    public static final int INVENTORY_CAPACITY = 10;

    private final HunterRepository hunterRepository;
    private final HunterInventoryItemRepository hunterInventoryItemRepository;

    @Transactional(readOnly = true)
    public ShopResponse getShop(String username) {
        Hunter hunter = getHunterOrThrow(username);
        List<HunterInventoryItem> inventoryItems = hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId());

        return new ShopResponse(
                hunter.getGold(),
                inventoryItems.size(),
                INVENTORY_CAPACITY,
                Arrays.stream(InventoryItemType.values()).map(ShopItemResponse::from).toList()
        );
    }

    @Transactional
    public PurchaseItemResponse purchaseItem(String username, PurchaseItemRequest request) {
        Hunter hunter = getHunterOrThrow(username);
        List<HunterInventoryItem> inventoryItems = hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId());

        if (inventoryItems.size() >= INVENTORY_CAPACITY) {
            throw new InvalidGameRuleException("Backpack is full");
        }

        InventoryItemType itemType = request.itemType();
        if (hunter.getGold() < itemType.getPrice()) {
            throw new InvalidGameRuleException("Not enough gold to buy this item");
        }

        int nextSlot = findNextSlot(inventoryItems);
        HunterInventoryItem savedItem = hunterInventoryItemRepository.save(HunterInventoryItem.builder()
                .hunter(hunter)
                .itemType(itemType)
                .slotIndex(nextSlot)
                .build());

        hunter.setGold(hunter.getGold() - itemType.getPrice());

        HunterResponse hunterResponse = HunterResponse.from(hunter, hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId()), INVENTORY_CAPACITY);

        return new PurchaseItemResponse(
                hunterResponse,
                InventoryItemResponse.from(savedItem),
                hunter.getGold(),
                hunterResponse.inventory().size(),
                hunterResponse.inventoryCapacity(),
                itemType.getDisplayName() + " added to backpack"
        );
    }

    private Hunter getHunterOrThrow(String username) {
        return hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));
    }

    private int findNextSlot(List<HunterInventoryItem> inventoryItems) {
        boolean[] usedSlots = new boolean[INVENTORY_CAPACITY];
        for (HunterInventoryItem item : inventoryItems) {
            if (item.getSlotIndex() >= 0 && item.getSlotIndex() < INVENTORY_CAPACITY) {
                usedSlots[item.getSlotIndex()] = true;
            }
        }

        for (int slotIndex = 0; slotIndex < INVENTORY_CAPACITY; slotIndex++) {
            if (!usedSlots[slotIndex]) {
                return slotIndex;
            }
        }

        throw new InvalidGameRuleException("Backpack is full");
    }
}
