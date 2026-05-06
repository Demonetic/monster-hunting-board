package se.edugrade.monsterhuntingboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.InventoryActionResponse;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemRequest;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopResponse;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.InventoryItemType;
import se.edugrade.monsterhuntingboard.model.Role;
import se.edugrade.monsterhuntingboard.model.UserAccount;
import se.edugrade.monsterhuntingboard.repository.UserAccountRepository;
import se.edugrade.monsterhuntingboard.util.TestIds;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShopServiceTest {

    @Autowired
    private ShopService shopService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;

    @BeforeEach
    void setUp() {
        username = "shop-" + TestIds.shortId();

        UserAccount userAccount = UserAccount.builder()
                .username(username)
                .password(passwordEncoder.encode("password123"))
                .role(Role.HUNTER)
                .build();

        Hunter hunter = Hunter.builder()
                .displayName("Aria")
                .appearance(Appearance.MAGE)
                .level(1)
                .exp(0)
                .gold(500)
                .baseHp(100)
                .currentHp(100)
                .userAccount(userAccount)
                .build();

        userAccount.setHunter(hunter);
        hunter.setUserAccount(userAccount);
        userAccountRepository.save(userAccount);
    }

    @Test
    void getShopShowsPotionCatalogAndCapacity() {
        ShopResponse response = shopService.getShop(username);

        assertThat(response.hunterGold()).isEqualTo(500);
        assertThat(response.inventorySize()).isEqualTo(0);
        assertThat(response.inventoryCapacity()).isEqualTo(10);
        assertThat(response.items()).hasSize(3);
    }

    @Test
    void purchaseItemUsesGoldAndAddsBackpackEntry() {
        PurchaseItemResponse response = shopService.purchaseItem(
                username,
                new PurchaseItemRequest(InventoryItemType.EXP_POTION)
        );

        assertThat(response.purchasedItem().itemType()).isEqualTo(InventoryItemType.EXP_POTION);
        assertThat(response.remainingGold()).isEqualTo(455);
        assertThat(response.inventorySize()).isEqualTo(1);
        assertThat(response.hunter().inventory()).hasSize(1);
        assertThat(response.hunter().inventory().getFirst().slotIndex()).isEqualTo(0);
    }

    @Test
    void purchaseRejectsFullBackpackAndInsufficientGold() {
        for (int index = 0; index < ShopService.INVENTORY_CAPACITY; index++) {
            shopService.purchaseItem(username, new PurchaseItemRequest(InventoryItemType.HEALTH_POTION));
        }

        assertThatThrownBy(() -> shopService.purchaseItem(
                username,
                new PurchaseItemRequest(InventoryItemType.HEALTH_POTION)
        )).isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("Backpack is full");

        String poorUsername = "poor-" + TestIds.shortId();
        UserAccount poorAccount = UserAccount.builder()
                .username(poorUsername)
                .password(passwordEncoder.encode("password123"))
                .role(Role.HUNTER)
                .build();
        Hunter poorHunter = Hunter.builder()
                .displayName("Poor Hunter")
                .appearance(Appearance.HUNTER)
                .level(1)
                .exp(0)
                .gold(10)
                .baseHp(100)
                .currentHp(100)
                .userAccount(poorAccount)
                .build();
        poorAccount.setHunter(poorHunter);
        poorHunter.setUserAccount(poorAccount);
        userAccountRepository.save(poorAccount);

        assertThatThrownBy(() -> shopService.purchaseItem(
                poorUsername,
                new PurchaseItemRequest(InventoryItemType.ENDURANCE_POTION)
        )).isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("Not enough gold");
    }

    @Test
    void potionsCanBeUsedOrDiscardedWithRulesEnforced() {
        PurchaseItemResponse healthPurchase = shopService.purchaseItem(
                username,
                new PurchaseItemRequest(InventoryItemType.HEALTH_POTION)
        );
        PurchaseItemResponse expPurchase = shopService.purchaseItem(
                username,
                new PurchaseItemRequest(InventoryItemType.EXP_POTION)
        );

        assertThatThrownBy(() -> shopService.useInventoryItem(username, healthPurchase.purchasedItem().id()))
                .isInstanceOf(InvalidGameRuleException.class)
                .hasMessageContaining("max HP");

        UserAccount account = userAccountRepository.findByUsername(username).orElseThrow();
        account.getHunter().setCurrentHp(40);

        InventoryActionResponse healthResponse = shopService.useInventoryItem(username, healthPurchase.purchasedItem().id());
        assertThat(healthResponse.hunter().currentHp()).isEqualTo(90);

        InventoryActionResponse expResponse = shopService.useInventoryItem(username, expPurchase.purchasedItem().id());
        assertThat(expResponse.hunter().expPotionActive()).isTrue();

        PurchaseItemResponse endurancePurchase = shopService.purchaseItem(
                username,
                new PurchaseItemRequest(InventoryItemType.ENDURANCE_POTION)
        );
        InventoryActionResponse discardResponse = shopService.discardInventoryItem(username, endurancePurchase.purchasedItem().id());
        assertThat(discardResponse.message()).contains("discarded");
    }
}
