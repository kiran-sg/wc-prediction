package com.wc.prediction.wcprediction.service.impl;

import com.wc.prediction.wcprediction.dto.LeaderboardDTO;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import com.wc.prediction.wcprediction.entity.Prediction;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.entity.WcMatch;
import com.wc.prediction.wcprediction.repository.MatchRepository;
import com.wc.prediction.wcprediction.repository.PredictionRepository;
import com.wc.prediction.wcprediction.repository.UserRepository;
import com.wc.prediction.wcprediction.response.PredictionResponse;
import com.wc.prediction.wcprediction.service.PredictionService;
import com.wc.prediction.wcprediction.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.wc.prediction.wcprediction.util.MapperUtil.*;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {

    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MatchRepository matchRepository;

    private final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MMM");

    private static LocalDateTime parseMatchDateTime(String dateTime) {
        if (dateTime.contains("T")) {
            return OffsetDateTime.parse(dateTime).toLocalDateTime();
        }
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public PredictionDto getPrediction(String userId, String matchId) {
        WcUser user = userRepository.findByUserId(userId);
        Optional<Prediction> prediction = predictionRepository.findByUserAndMatchId(user, matchId);
        return prediction.map(MapperUtil::predictionToPredictionDto).orElse(null);
    }

    @Override
    public PredictionDto savePrediction(PredictionDto predictionDto, String userId) {
        WcUser user = userRepository.findByUserId(predictionDto.getUserId());
        Optional<Prediction> existing = predictionRepository
                .findByUserAndMatchId(user, predictionDto.getMatchId());

        if (existing.isPresent()) {
            Prediction pred = existing.get();
            updatePrediction(pred, predictionDto);
            return predictionToPredictionDto(predictionRepository.save(pred));
        } else {
            Prediction newPred = new Prediction();
            updatePrediction(newPred, predictionDto);
            newPred.setMatchId(predictionDto.getMatchId());
            newPred.setUser(user);
            return predictionToPredictionDto(predictionRepository.save(newPred));
        }
    }

    @Override
    public List<PredictionDto> getPredictionsByUser(String userId) {
        WcUser user = userRepository.findByUserId(userId);
        Optional<List<Prediction>> predictions = predictionRepository.findAllByUser(user);
        List<WcMatch> matches = matchRepository.findAll();
        List<PredictionDto> dtoList = new ArrayList<>();

        predictions.ifPresent(list -> list.forEach(p ->
                dtoList.add(MapperUtil.predictionToPredictionDto(p))));

        dtoList.forEach(dto -> matches.stream()
                .filter(m -> Objects.equals(dto.getMatchId(), m.getMatchNo()))
                .findFirst()
                .ifPresent(m -> {
                    dto.setMatch(m.getTeamA() + " vs " + m.getTeamB());
                    dto.setMatchDateTime(m.getDateTime());
                    dto.setMatchDate(parseMatchDateTime(m.getDateTime()).format(outputFormatter));
                }));

        return dtoList.stream()
                .sorted(Comparator.comparing(d ->
                        parseMatchDateTime(d.getMatchDateTime()), Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public PredictionResponse getPredictionsForUserByMatches(String userId, List<String> matchIds) {
        PredictionResponse response = new PredictionResponse();
        WcUser user = userRepository.findByUserId(userId);
        Optional<List<Prediction>> predictions = predictionRepository.findAllByUserAndMatchIdIn(user, matchIds);
        List<PredictionDto> dtoList = new ArrayList<>();
        predictions.ifPresent(list -> list.forEach(p -> {
            PredictionDto dto = new PredictionDto();
            dto.setPredictionId(p.getPredictionId());
            dto.setMatchId(p.getMatchId());
            dto.setUserId(p.getUser().getUserId());
            dtoList.add(dto);
        }));
        response.setPredictions(dtoList);
        return response;
    }

    @Override
    public List<LeaderboardDTO> getLeaderboard(String location) {
        List<Object[]> results = predictionRepository.getLeaderboardByLocation(location);
        List<LeaderboardDTO> leaderboard = new ArrayList<>();
        int rank = 1;
        int position = 1;

        for (int i = 0; i < results.size(); i++) {
            Object[] result = results.get(i);
            WcUser user = (WcUser) result[0];
            int totalPoints = ((Number) result[1]).intValue();

            if (i > 0 && totalPoints != ((Number) results.get(i - 1)[1]).intValue()) {
                rank = position;
            }

            leaderboard.add(new LeaderboardDTO(
                    user.getUserId(), user.getName(), user.getLocation(), totalPoints, rank));
            position++;
        }

        return leaderboard.stream()
                .filter(d -> d.getTotalPoints() > 0)
                .toList();
    }
}
