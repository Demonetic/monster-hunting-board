package se.edugrade.monsterhuntingboard.service;

import java.util.List;
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
public class BeastService {

    private final BeastRepository beastRepository;
    private final HuntRepository huntRepository;

    public BeastService(BeastRepository beastRepository, HuntRepository huntRepository) {
        this.beastRepository = beastRepository;
        this.huntRepository = huntRepository;
    }

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
        return toResponse(savedBeast);
    }

    @Transactional(readOnly = true)
    public List<BeastResponse> getAllBeasts() {
        return beastRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeastResponse getBeastById(Long id) {
        Beast beast = beastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Beast not found with id: " + id));
        return toResponse(beast);
    }

    @Transactional(readOnly = true)
    public List<BeastResponse> getBeastsByDifficulty(Difficulty difficulty) {
        return beastRepository.findByDifficulty(difficulty)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BeastResponse> getBeastsByType(BeastType type) {
        return beastRepository.findByType(type)
                .stream()
                .map(this::toResponse)
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

        return toResponse(beastRepository.save(beast));
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

    private BeastResponse toResponse(Beast beast) {
        return new BeastResponse(
                beast.getId(),
                beast.getType(),
                beast.getDifficulty(),
                beast.getHp(),
                beast.getAttackPower(),
                beast.getRewardExp(),
                beast.getRewardGold()
        );
    }
}
