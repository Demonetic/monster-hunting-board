package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemRequest;
import se.edugrade.monsterhuntingboard.dto.PurchaseItemResponse;
import se.edugrade.monsterhuntingboard.dto.ShopResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.service.HunterService;
import se.edugrade.monsterhuntingboard.service.ShopService;

@RestController
@RequestMapping("/api/hunters")
public class HunterController {

    private final HunterService hunterService;
    private final ShopService shopService;

    public HunterController(HunterService hunterService, ShopService shopService) {
        this.hunterService = hunterService;
        this.shopService = shopService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<HunterResponse> getCurrentHunter(Principal principal) {
        return ResponseEntity.ok(hunterService.getCurrentHunter(principal.getName()));
    }

    @PatchMapping("/me/appearance")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<HunterResponse> updateAppearance(
            Principal principal,
            @Valid @RequestBody UpdateAppearanceRequest request
    ) {
        return ResponseEntity.ok(hunterService.updateAppearance(principal.getName(), request));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<List<HunterResponse>> getAllHunters() {
        return ResponseEntity.ok(hunterService.getAllHunters());
    }

    @GetMapping("/me/shop")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<ShopResponse> getShop(Principal principal) {
        return ResponseEntity.ok(shopService.getShop(principal.getName()));
    }

    @PostMapping("/me/shop/purchase")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<PurchaseItemResponse> purchaseItem(
            Principal principal,
            @Valid @RequestBody PurchaseItemRequest request
    ) {
        return ResponseEntity.ok(shopService.purchaseItem(principal.getName(), request));
    }
}
