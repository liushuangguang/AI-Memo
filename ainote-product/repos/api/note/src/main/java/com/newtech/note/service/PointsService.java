package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.entity.dto.PointsChangeEnum;
import com.newtech.note.entity.dto.PointsDataEnum;
import com.newtech.note.entity.dto.UserPoints;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PointsService {

    /**
     * Backend-only ledger update. Callers must supply a positive user id and exactly the
     * fields required by the selected change method; invalid combinations fail with an error.
     */
    Mono<NoteBaseResponse<Void>> updatePoints(Long uid,String deviceId, PointsChangeEnum changeMethod, Double changePoint,Long token, PointsDataEnum days);

    Mono<Double> getAvailablePoints(Long uid, String deviceId);

    Mono<List<UserPoints>> getPointsUpdateHistory(Long uid);
}
