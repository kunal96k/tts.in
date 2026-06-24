package com.tts.enquiries.service;

import com.tts.enquiries.entity.Prize;
import com.tts.enquiries.repository.PrizeRepository;
import com.tts.enquiries.repository.SpinDrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrizeService {

    private final PrizeRepository prizeRepository;
    private final SpinDrawRepository spinDrawRepository;

    public List<Prize> getAllPrizes() {
        return prizeRepository.findAll();
    }

    public Optional<Prize> getPrizeById(Long id) {
        return prizeRepository.findById(id);
    }

    @Transactional
    public Prize createPrize(Prize prize) {
        // Validate no duplicate names
        if (prizeRepository.findByName(prize.getName()).isPresent()) {
            throw new IllegalArgumentException("A prize with the name '" + prize.getName() + "' already exists.");
        }
        return prizeRepository.save(prize);
    }

    @Transactional
    public Prize updatePrize(Long id, Prize updated) {
        Prize existing = prizeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prize not found with id: " + id));

        // Check name uniqueness if name changed
        if (!existing.getName().equals(updated.getName())) {
            prizeRepository.findByName(updated.getName()).ifPresent(p -> {
                throw new IllegalArgumentException("A prize with the name '" + updated.getName() + "' already exists.");
            });
            existing.setName(updated.getName());
        }

        existing.setType(updated.getType());
        existing.setTotalVolume(updated.getTotalVolume());
        existing.setWinProbability(updated.getWinProbability());
        existing.setStatus(updated.getStatus());
        return prizeRepository.save(existing);
    }

    @Transactional
    public void deletePrize(Long id) {
        if (!prizeRepository.existsById(id)) {
            throw new IllegalArgumentException("Prize not found with id: " + id);
        }
        prizeRepository.deleteById(id);
    }

    /**
     * Toggle prize status between Active and Inactive.
     */
    @Transactional
    public Prize togglePrizeStatus(Long id) {
        Prize prize = prizeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prize not found with id: " + id));
        prize.setStatus(prize.getStatus().equalsIgnoreCase("Active") ? "Inactive" : "Active");
        return prizeRepository.save(prize);
    }

    /**
     * Returns how many times each prize has been won (from spin_draws table).
     */
    public long getWonCount(String prizeName) {
        return spinDrawRepository.countByPrizeWon(prizeName);
    }
}
