package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.service.HunterService;

@RestController
@RequestMapping("/api/hunters")
public class HunterController {

    private final HunterService hunterService;

    public HunterController(HunterService hunterService) {
        this.hunterService = hunterService;
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
}
