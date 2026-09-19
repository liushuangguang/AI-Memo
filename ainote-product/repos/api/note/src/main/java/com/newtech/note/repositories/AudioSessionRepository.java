package com.newtech.note.repositories;

import com.newtech.note.entity.dto.AudioSession;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface AudioSessionRepository extends R2dbcRepository<AudioSession, Long> {

}
