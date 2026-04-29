package se.edugrade.monsterhuntingboard.controller;

import jakarta.validation.Valid;
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
import se.edugrade.monsterhuntingboard.dto.BeastRequest;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateBeastRequest;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.service.BeastService;

@RestController
@RequestMapping("/api/beasts")
public class BeastController {

    private final BeastService beastService;

    public BeastController(BeastService beastService) {
        this.beastService = beastService;
    }

    @GetMapping
    public ResponseEntity<List<BeastResponse>> getAllBeasts() {
        return ResponseEntity.ok(beastService.getAllBeasts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BeastResponse> getBeastById(@PathVariable Long id) {
        return ResponseEntity.ok(beastService.getBeastById(id));
    }

    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<BeastResponse>> getBeastsByDifficulty(@PathVariable Difficulty difficulty) {
        return ResponseEntity.ok(beastService.getBeastsByDifficulty(difficulty));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<BeastResponse>> getBeastsByType(@PathVariable BeastType type) {
        return ResponseEntity.ok(beastService.getBeastsByType(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<BeastResponse> createBeast(@Valid @RequestBody BeastRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(beastService.createBeast(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<BeastResponse> updateBeast(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBeastRequest request
    ) {
        return ResponseEntity.ok(beastService.updateBeast(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GAME_MASTER')")
    public ResponseEntity<Void> deleteBeast(@PathVariable Long id) {
        beastService.deleteBeast(id);
        return ResponseEntity.noContent().build();
    }
}
