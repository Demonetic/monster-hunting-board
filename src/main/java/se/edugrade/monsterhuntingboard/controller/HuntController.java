package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.edugrade.monsterhuntingboard.dto.CompleteHuntRequest;
import se.edugrade.monsterhuntingboard.dto.CreateHuntRequest;
import se.edugrade.monsterhuntingboard.dto.GroupLobbyResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResponse;
import se.edugrade.monsterhuntingboard.dto.HuntResultResponse;
import se.edugrade.monsterhuntingboard.dto.JoinHuntResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateHuntRequest;
import se.edugrade.monsterhuntingboard.service.HuntService;

@RestController
@RequestMapping("/api/hunts")
public class HuntController {

    private final HuntService huntService;

    public HuntController(HuntService huntService) {
        this.huntService = huntService;
    }

    @GetMapping
    public ResponseEntity<List<HuntResponse>> getAllHunts(Principal principal) {
        return ResponseEntity.ok(huntService.getAllHunts(principal != null ? principal.getName() : null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HuntResponse> getHuntById(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(huntService.getHuntById(id, principal != null ? principal.getName() : null));
    }

    @GetMapping("/{id}/lobby")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<GroupLobbyResponse> getGroupLobby(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(huntService.getGroupLobby(id, principal.getName()));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<HuntResponse>> getScheduledHunts(Principal principal) {
        return ResponseEntity.ok(huntService.getScheduledHunts(principal != null ? principal.getName() : null));
    }

    @PostMapping
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<HuntResponse> createHunt(@Valid @RequestBody CreateHuntRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(huntService.createHunt(request));
    }

    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<JoinHuntResponse> joinHunt(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(huntService.joinHunt(id, principal.getName()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<HuntResultResponse> completeHunt(
            @PathVariable Long id,
            Principal principal,
            @Valid @RequestBody CompleteHuntRequest request
    ) {
        return ResponseEntity.ok(huntService.completeHuntForCurrentHunter(id, principal.getName(), request));
    }

    @PostMapping("/{id}/solo/start")
    @PreAuthorize("hasRole('HUNTER')")
    public ResponseEntity<HuntResultResponse> startSoloHunt(
            @PathVariable Long id,
            Principal principal,
            @Valid @RequestBody CompleteHuntRequest request
    ) {
        return ResponseEntity.ok(huntService.startSoloHunt(id, principal.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<HuntResponse> updateHunt(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHuntRequest request
    ) {
        return ResponseEntity.ok(huntService.updateHunt(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<Void> deleteHunt(@PathVariable Long id) {
        huntService.deleteHunt(id);
        return ResponseEntity.noContent().build();
    }
}
