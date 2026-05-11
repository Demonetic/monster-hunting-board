package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BeastRequest;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateBeastRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;

@Service
@RequiredArgsConstructor
public class BeastService {
    private static final Logger log = LoggerFactory.getLogger(BeastService.class);

    private final BeastRepository beastRepository;
    private final HuntRepository huntRepository;

    @Transactional
    public BeastResponse createBeast(BeastRequest request) {
        Beast beast = Beast.builder()
                .name(request.name().trim())
                .type(request.type())
                .hp(request.hp())
                .attackPower(request.attackPower())
                .rewardExp(request.rewardExp())
                .rewardGold(request.rewardGold())
                .build();

        Beast savedBeast = beastRepository.save(beast);
        log.info("Created beast: {} (id={})", savedBeast.getType(), savedBeast.getId());
        return BeastResponse.from(savedBeast);
    }

    @Transactional(readOnly = true)
    public List<BeastResponse> getAllBeasts() {
        return beastRepository.findAll()
                .stream()
                .map(BeastResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeastResponse getBeastById(Long id) {
        Beast beast = beastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beast not found with id: " + id));
        return BeastResponse.from(beast);
    }

    @Transactional(readOnly = true)
    public List<BeastResponse> getBeastsByType(BeastType type) {
        return beastRepository.findByType(type)
                .stream()
                .map(BeastResponse::from)
                .toList();
    }

    @Transactional
    public BeastResponse updateBeast(Long id, UpdateBeastRequest request) {
        Beast beast = getBeastOrThrow(id);

        if (request.name() != null) {
            beast.setName(request.name().trim());
        }
        if (request.type() != null) {
            beast.setType(request.type());
        }
        if (request.hp() != null) {
            beast.setHp(request.hp());
        }
        if (request.attackPower() != null) {
            beast.setAttackPower(request.attackPower());
        }
        if (request.rewardExp() != null) {
            beast.setRewardExp(request.rewardExp());
        }
        if (request.rewardGold() != null) {
            beast.setRewardGold(request.rewardGold());
        }

        Beast savedBeast = beastRepository.save(beast);
        log.info("Updated beast id={}", savedBeast.getId());
        return BeastResponse.from(savedBeast);
    }

    @Transactional
    public void deleteBeast(Long id) {
        Beast beast = getBeastOrThrow(id);

        if (huntRepository.existsByBeastsId(id)) {
            throw new InvalidGameRuleException("Cannot delete beast because it is used in one or more hunts");
        }

        beastRepository.delete(beast);
        log.info("Deleted beast id={}", id);
    }

    private Beast getBeastOrThrow(Long id) {
        return beastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beast not found with id: " + id));
    }
}
