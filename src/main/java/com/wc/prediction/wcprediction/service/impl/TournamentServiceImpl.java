package com.wc.prediction.wcprediction.service.impl;

import com.wc.prediction.wcprediction.dto.TournamentPredictionDto;
import com.wc.prediction.wcprediction.dto.TournamentResultDto;
import com.wc.prediction.wcprediction.entity.AppConfig;
import com.wc.prediction.wcprediction.entity.TournamentPrediction;
import com.wc.prediction.wcprediction.entity.TournamentResult;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.repository.AppConfigRepository;
import com.wc.prediction.wcprediction.repository.TournamentPredictionRepository;
import com.wc.prediction.wcprediction.repository.TournamentResultRepository;
import com.wc.prediction.wcprediction.repository.UserRepository;
import com.wc.prediction.wcprediction.service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TournamentServiceImpl implements TournamentService {

    @Autowired
    private TournamentPredictionRepository predictionRepo;
    @Autowired
    private TournamentResultRepository resultRepo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AppConfigRepository appConfigRepo;

    private static final String TOURNAMENT_OPEN_KEY = "tournament_predictions_open";

    @Override
    public TournamentPredictionDto getPrediction(String userId) {
        WcUser user = userRepository.findByUserId(userId);
        if (user == null) return null;
        return predictionRepo.findByUser(user).map(this::toDto).orElse(null);
    }

    @Override
    @Transactional
    public TournamentPredictionDto savePrediction(String userId, TournamentPredictionDto dto) {
        // Enforce open/close gate
        boolean open = appConfigRepo.findByConfigKey(TOURNAMENT_OPEN_KEY)
            .map(c -> Boolean.TRUE.equals(c.getConfigValue()))
            .orElse(true);
        if (!open) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tournament predictions are closed");
        }

        WcUser user = userRepository.findByUserId(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        TournamentPrediction pred = predictionRepo.findByUser(user).orElse(new TournamentPrediction());
        pred.setUser(user);
        pred.setGoldenBall(dto.getGoldenBall() != null ? dto.getGoldenBall().trim() : null);
        pred.setGoldenBoot(dto.getGoldenBoot() != null ? dto.getGoldenBoot().trim() : null);
        pred.setGoldenGlove(dto.getGoldenGlove() != null ? dto.getGoldenGlove().trim() : null);
        pred.setYoungPlayer(dto.getYoungPlayer() != null ? dto.getYoungPlayer().trim() : null);
        pred.setFairPlayTeam(dto.getFairPlayTeam() != null ? dto.getFairPlayTeam().trim() : null);
        pred.setPredictionTime(LocalDateTime.now());

        // Points stay at 0 until admin saves results — do not score on save
        return toDto(predictionRepo.save(pred));
    }

    @Override
    public TournamentResultDto getResult() {
        return resultRepo.findFirstByOrderByIdAsc().map(this::toResultDto).orElse(new TournamentResultDto());
    }

    @Override
    @Transactional
    public TournamentResultDto saveResult(TournamentResultDto dto) {
        // Gate: predictions must be closed before results can be saved
        boolean open = appConfigRepo.findByConfigKey(TOURNAMENT_OPEN_KEY)
            .map(c -> Boolean.TRUE.equals(c.getConfigValue()))
            .orElse(true);
        if (open) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Close tournament predictions before saving results");
        }

        TournamentResult result = resultRepo.findFirstByOrderByIdAsc().orElse(new TournamentResult());
        result.setGoldenBall(dto.getGoldenBall() != null ? dto.getGoldenBall().trim() : null);
        result.setGoldenBoot(dto.getGoldenBoot() != null ? dto.getGoldenBoot().trim() : null);
        result.setGoldenGlove(dto.getGoldenGlove() != null ? dto.getGoldenGlove().trim() : null);
        result.setYoungPlayer(dto.getYoungPlayer() != null ? dto.getYoungPlayer().trim() : null);
        result.setFairPlayTeam(dto.getFairPlayTeam() != null ? dto.getFairPlayTeam().trim() : null);
        result.setUpdatedTime(LocalDateTime.now());
        resultRepo.save(result);
        recalculatePoints();
        return toResultDto(result);
    }

    @Override
    @Transactional
    public void recalculatePoints() {
        resultRepo.findFirstByOrderByIdAsc().ifPresent(result -> {
            List<TournamentPrediction> all = predictionRepo.findAll();
            all.forEach(p -> recalculate(p, result));
            predictionRepo.saveAll(all);
        });
    }

    @Override
    public List<TournamentPredictionDto> getAllPredictions() {
        return predictionRepo.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public WcUser findUser(String userId) {
        return userRepository.findByUserId(userId);
    }

    private void recalculate(TournamentPrediction pred, TournamentResult result) {
        int gb = matches(pred.getGoldenBall(), result.getGoldenBall()) ? 3 : 0;
        int gbo = matches(pred.getGoldenBoot(), result.getGoldenBoot()) ? 3 : 0;
        int gg = matches(pred.getGoldenGlove(), result.getGoldenGlove()) ? 3 : 0;
        int yp = matches(pred.getYoungPlayer(), result.getYoungPlayer()) ? 3 : 0;
        int fp = matches(pred.getFairPlayTeam(), result.getFairPlayTeam()) ? 3 : 0;
        pred.setGoldenBallPoints(gb);
        pred.setGoldenBootPoints(gbo);
        pred.setGoldenGlovePoints(gg);
        pred.setYoungPlayerPoints(yp);
        pred.setFairPlayPoints(fp);
        pred.setTotalPoints(gb + gbo + gg + yp + fp);
    }

    private boolean matches(String predicted, String actual) {
        return predicted != null && actual != null && !actual.isBlank()
            && predicted.trim().equalsIgnoreCase(actual.trim());
    }

    private TournamentPredictionDto toDto(TournamentPrediction p) {
        TournamentPredictionDto dto = new TournamentPredictionDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUser().getUserId());
        dto.setUserName(p.getUser().getName());
        dto.setGoldenBall(p.getGoldenBall());
        dto.setGoldenBoot(p.getGoldenBoot());
        dto.setGoldenGlove(p.getGoldenGlove());
        dto.setYoungPlayer(p.getYoungPlayer());
        dto.setFairPlayTeam(p.getFairPlayTeam());
        dto.setGoldenBallPoints(p.getGoldenBallPoints() == null ? 0 : p.getGoldenBallPoints());
        dto.setGoldenBootPoints(p.getGoldenBootPoints() == null ? 0 : p.getGoldenBootPoints());
        dto.setGoldenGlovePoints(p.getGoldenGlovePoints() == null ? 0 : p.getGoldenGlovePoints());
        dto.setYoungPlayerPoints(p.getYoungPlayerPoints() == null ? 0 : p.getYoungPlayerPoints());
        dto.setFairPlayPoints(p.getFairPlayPoints() == null ? 0 : p.getFairPlayPoints());
        dto.setTotalPoints(p.getTotalPoints() == null ? 0 : p.getTotalPoints());
        return dto;
    }

    private TournamentResultDto toResultDto(TournamentResult r) {
        TournamentResultDto dto = new TournamentResultDto();
        dto.setGoldenBall(r.getGoldenBall());
        dto.setGoldenBoot(r.getGoldenBoot());
        dto.setGoldenGlove(r.getGoldenGlove());
        dto.setYoungPlayer(r.getYoungPlayer());
        dto.setFairPlayTeam(r.getFairPlayTeam());
        return dto;
    }
}
