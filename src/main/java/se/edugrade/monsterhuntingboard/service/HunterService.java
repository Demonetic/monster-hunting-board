package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.repository.HunterInventoryItemRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
@RequiredArgsConstructor
public class HunterService {
    private static final Logger log = LoggerFactory.getLogger(HunterService.class);

    private final HunterRepository hunterRepository;
    private final HunterInventoryItemRepository hunterInventoryItemRepository;

    @Transactional(readOnly = true)
    public HunterResponse getCurrentHunter(String username) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));
        return HunterResponse.from(
                hunter,
                hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId()),
                ShopService.INVENTORY_CAPACITY
        );
    }

    @Transactional
    public HunterResponse updateAppearance(String username, UpdateAppearanceRequest request) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));

        if (request.appearance() == Appearance.BARD) {
            throw new InvalidGameRuleException("Hunters cannot use appearance BARD");
        }

        hunter.setAppearance(request.appearance());
        log.info("Updated hunter appearance: {} -> {}", username, hunter.getAppearance());
        return HunterResponse.from(
                hunter,
                hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId()),
                ShopService.INVENTORY_CAPACITY
        );
    }

    @Transactional(readOnly = true)
    public List<HunterResponse> getAllHunters() {
        return hunterRepository.findAll()
                .stream()
                .map(hunter -> HunterResponse.from(
                        hunter,
                        hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId()),
                        ShopService.INVENTORY_CAPACITY
                ))
                .toList();
    }
}
