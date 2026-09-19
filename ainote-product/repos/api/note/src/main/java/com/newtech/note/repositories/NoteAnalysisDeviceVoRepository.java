package com.newtech.note.repositories;

import com.newtech.note.entity.vo.NoteAnalysisDeviceVo;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface NoteAnalysisDeviceVoRepository extends R2dbcRepository<NoteAnalysisDeviceVo, Long> {
    @Query("select a.*, b.id bid, b.device_id, b.device_name, b.created_at device_created_at, b.updated_at device_updated_at from note_analysis a " +
            "inner join note_device b " +
            "on a.device_id = b.device_id " +
            "where a.id = :id")
    Mono<NoteAnalysisDeviceVo> findNoteAnalysisById(@Param("id") Long id);
}
