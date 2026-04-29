package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.HunterResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateAppearanceRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Appearance;
import se.edugrade.monsterhuntingboard.model.Hunter;
import se.edugrade.monsterhuntingboard.repository.HunterRepository;

@Service
public class HunterService {

    private final HunterRepository hunterRepository;

    public HunterService(HunterRepository hunterRepository) {
        this.hunterRepository = hunterRepository;
    }

    @Transactional(readOnly = true)
    public HunterResponse getCurrentHunter(String username) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));
        return toResponse(hunter);
    }

    @Transactional
    public HunterResponse updateAppearance(String username, UpdateAppearanceRequest request) {
        Hunter hunter = hunterRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Hunter not found for username: " + username));

        if (request.appearance() == Appearance.BARD) {
            throw new InvalidGameRuleException("Hunters cannot use appearance BARD");
        }

        hunter.setAppearance(request.appearance());
        return toResponse(hunter);
    }

    @Transactional(readOnly = true)
    public List<HunterResponse> getAllHunters() {
        return hunterRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private HunterResponse toResponse(Hunter hunter) {
        return new HunterResponse(
                hunter.getId(),
                hunter.getDisplayName(),
                hunter.getAppearance(),
                hunter.getLevel(),
                hunter.getExp(),
                hunter.getGold(),
                hunter.getBaseHp(),
                hunter.getCurrentHp()
        );
    }
}
