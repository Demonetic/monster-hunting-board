package se.edugrade.monsterhuntingboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
                new HunterResponse(1L, "Aria", Appearance.MAGE, 1, 0, 0, 100, 100, 10, List.of())
        );

        mockMvc.perform(get("/api/hunters/me")
                        .with(user("aria").roles("HUNTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Aria"));
    }

    @Test
    void patchAppearanceReturnsOkAndBardIsRejected() throws Exception {
        when(hunterService.updateAppearance(eq("aria"), any())).thenReturn(
                new HunterResponse(1L, "Aria", Appearance.PALADIN, 1, 0, 0, 100, 100, 10, List.of())
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
                new HunterResponse(1L, "Aria", Appearance.MAGE, 1, 0, 0, 100, 100, 10, List.of())
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
                new HunterResponse(1L, "Aria", Appearance.MAGE, 1, 0, 90, 100, 100, 10, List.of()),
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
}
