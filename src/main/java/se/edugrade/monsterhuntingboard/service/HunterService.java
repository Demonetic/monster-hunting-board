package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateLocationRequest;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.model.ResolvedLocation;
import se.edugrade.monsterhuntingboard.repository.HunterInventoryItemRepository;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;
import se.edugrade.monsterhuntingboard.util.GameBalanceUtil;

@Service
@RequiredArgsConstructor
public class HunterService {
    private static final Logger log = LoggerFactory.getLogger(HunterService.class);

    private final HunterRepository hunterRepository;
    private final HunterInventoryItemRepository hunterInventoryItemRepository;
    private final WeatherService weatherService;

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

        int previousBaseHp = hunter.getBaseHp();
        int damageTaken = Math.max(0, previousBaseHp - hunter.getCurrentHp());
        hunter.setAppearance(request.appearance());
        int newBaseHp = GameBalanceUtil.calculateBaseHp(hunter.getLevel(), hunter.getAppearance());
        hunter.setBaseHp(newBaseHp);
        hunter.setCurrentHp(Math.max(0, newBaseHp - damageTaken));
        log.info("Updated hunter appearance: {} -> {}", username, hunter.getAppearance());
        return HunterResponse.from(
                hunter,
                hunterInventoryItemRepository.findByHunterIdOrderBySlotIndexAsc(hunter.getId()),
                ShopService.INVENTORY_CAPACITY
        );
    }

    @Transactional
    public HunterResponse updateLocation(String username, UpdateLocationRequest request) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));

        ResolvedLocation location = weatherService.resolveCity(request.city());
        hunter.setCity(location.city());
        hunter.setCountry(location.country());
        hunter.setLatitude(location.latitude());
        hunter.setLongitude(location.longitude());
        log.info("Updated hunter location: {} -> {}, {}", username, location.city(), location.country());

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
