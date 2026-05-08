package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.edugrade.monsterhuntingboard.config.SecurityConfig;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.InventoryActionResponse;
import se.edugrade.monsterhuntingboard.dto.InventoryItemResponse;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopResponse;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.InventoryItemType;
import se.edugrade.monsterhuntingboard.security.CustomUserDetailsService;
import se.edugrade.monsterhuntingboard.security.JwtAuthenticationFilter;
import se.edugrade.monsterhuntingboard.security.JwtService;
import se.edugrade.monsterhuntingboard.service.HunterService;
import se.edugrade.monsterhuntingboard.service.ShopService;

@WebMvcTest(HunterController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HunterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HunterService hunterService;

    @MockitoBean
    private ShopService shopService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getCurrentHunterWithTokenReturnsOk() throws Exception {
        when(hunterService.getCurrentHunter("aria")).thenReturn(
                hunterResponse(Appearance.MAGE, false, false)
        );

        mockMvc.perform(get("/api/hunters/me")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Aria"));
    }

    @Test
    void patchAppearanceReturnsOkAndBardIsRejected() throws Exception {
        when(hunterService.updateAppearance(eq("aria"), any())).thenReturn(
                hunterResponse(Appearance.PALADIN, false, false)
        );

        mockMvc.perform(patch("/api/hunters/me/appearance")
                        .with(user("aria").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appearance": "PALADIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appearance").value("PALADIN"));

        mockMvc.perform(patch("/api/hunters/me/appearance")
                        .with(user("aria").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appearance": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hunterAndGameMasterSeeCorrectHunterEndpointPermissions() throws Exception {
        when(hunterService.getAllHunters()).thenReturn(List.of(
                hunterResponse(Appearance.MAGE, false, false)
        ));

        mockMvc.perform(get("/api/hunters/me")
                        .with(user("gm").roles("GAME_MASTER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/hunters/admin/all")
                        .with(user("gm").roles("GAME_MASTER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/hunters/admin/all")
                        .with(user("hunter").roles("HUNTER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shopEndpointsReturnCatalogAndPurchaseResult() throws Exception {
        when(shopService.getShop("aria")).thenReturn(new ShopResponse(
                120,
                0,
                10,
                List.of(ShopItemResponse.from(InventoryItemType.HEALTH_POTION))
        ));
        when(shopService.purchaseItem(eq("aria"), any())).thenReturn(new PurchaseItemResponse(
                hunterResponseWithLocation(Appearance.MAGE, false, false, "Stockholm", "Sweden", 59.3293, 18.0686, 90, 100, 100),
                new InventoryItemResponse(1L, 0, InventoryItemType.HEALTH_POTION, "Health Potion", "Restores lost HP after battle", 30, null),
                90,
                1,
                10,
                "Health Potion added to backpack"
        ));

        mockMvc.perform(get("/api/hunters/me/shop")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hunterGold").value(120))
                .andExpect(jsonPath("$.items[0].itemType").value("HEALTH_POTION"));

        mockMvc.perform(post("/api/hunters/me/shop/purchase")
                        .with(user("aria").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemType": "HEALTH_POTION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingGold").value(90))
                .andExpect(jsonPath("$.purchasedItem.slotIndex").value(0));
    }

    @Test
    void inventoryUseAndDiscardEndpointsReturnUpdatedHunter() throws Exception {
        when(shopService.useInventoryItem("aria", 7L)).thenReturn(new InventoryActionResponse(
                hunterResponseWithLocation(Appearance.MAGE, true, false, "Stockholm", "Sweden", 59.3293, 18.0686, 90, 100, 80),
                "EXP Potion activated for the next hunt"
        ));
        when(shopService.discardInventoryItem("aria", 8L)).thenReturn(new InventoryActionResponse(
                hunterResponseWithLocation(Appearance.MAGE, false, false, "Stockholm", "Sweden", 59.3293, 18.0686, 90, 100, 80),
                "Health Potion discarded"
        ));

        mockMvc.perform(post("/api/hunters/me/inventory/7/use")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hunter.expPotionActive").value(true));

        mockMvc.perform(delete("/api/hunters/me/inventory/8")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Health Potion discarded"));
    }

    @Test
    void updateLocationReturnsUpdatedHunter() throws Exception {
        when(hunterService.updateLocation(eq("aria"), any())).thenReturn(
                hunterResponseWithLocation(Appearance.MAGE, false, false, "Berlin", "Germany", 52.52, 13.40, 0, 100, 100)
        );

        mockMvc.perform(post("/api/hunters/me/location")
                        .with(user("aria").roles("HUNTER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "city": "Berlin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Berlin"))
                .andExpect(jsonPath("$.country").value("Germany"));
    }

    private HunterResponse hunterResponse(Appearance appearance, boolean expPotionActive, boolean endurancePotionActive) {
        return hunterResponseWithLocation(
                appearance,
                expPotionActive,
                endurancePotionActive,
                "Stockholm",
                "Sweden",
                59.3293,
                18.0686,
                0,
                100,
                100
        );
    }

    private HunterResponse hunterResponseWithLocation(
            Appearance appearance,
            boolean expPotionActive,
            boolean endurancePotionActive,
            String city,
            String country,
            double latitude,
            double longitude,
            int gold,
            int baseHp,
            int currentHp
    ) {
        return new HunterResponse(
                1L,
                "Aria",
                appearance,
                appearance.getDisplayName(),
                appearance.getPassiveSkillName(),
                appearance.getPassiveSkillDescription(),
                city,
                country,
                latitude,
                longitude,
                1,
                0,
                gold,
                baseHp,
                currentHp,
                expPotionActive,
                endurancePotionActive,
                10,
                List.of()
        );
    }
}
