package se.edugrade.monsterhuntingboard.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.edugrade.monsterhuntingboard.dto.BeastRequest;
import se.edugrade.monsterhuntingboard.dto.BeastResponse;
import se.edugrade.monsterhuntingboard.dto.UpdateBeastRequest;
import se.edugrade.monsterhuntingboard.exception.InvalidGameRuleException;
import se.edugrade.monsterhuntingboard.exception.ResourceNotFoundException;
import se.edugrade.monsterhuntingboard.model.Beast;
import se.edugrade.monsterhuntingboard.model.BeastType;
import se.edugrade.monsterhuntingboard.model.Difficulty;
import se.edugrade.monsterhuntingboard.repository.BeastRepository;
import se.edugrade.monsterhuntingboard.repository.HuntRepository;

@Service
@RequiredArgsConstructor
public class BeastService {

    private final BeastRepository beastRepository;
    private final HuntRepository huntRepository;

    @Transactional
    public BeastResponse createBeast(BeastRequest request) {
        Beast beast = Beast.builder()
                .type(request.type())
                .difficulty(request.difficulty())
                .hp(request.hp())
                .attackPower(request.attackPower())
                .rewardExp(request.rewardExp())
                .rewardGold(request.rewardGold())
                .build();

        Beast savedBeast = beastRepository.save(beast);
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
    public List<BeastResponse> getBeastsByDifficulty(Difficulty difficulty) {
        return beastRepository.findByDifficulty(difficulty)
                .stream()
                .map(BeastResponse::from)
                .toList();
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

        if (request.type() != null) {
            beast.setType(request.type());
        }
        if (request.difficulty() != null) {
            beast.setDifficulty(request.difficulty());
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

        return BeastResponse.from(beastRepository.save(beast));
    }

    @Transactional
    public void deleteBeast(Long id) {
        Beast beast = getBeastOrThrow(id);

        if (huntRepository.existsByBeastsId(id)) {
            throw new InvalidGameRuleException("Cannot delete beast because it is used in one or more hunts");
        }

        beastRepository.delete(beast);
    }

    private Beast getBeastOrThrow(Long id) {
        return beastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beast not found with id: " + id));
    }
}
