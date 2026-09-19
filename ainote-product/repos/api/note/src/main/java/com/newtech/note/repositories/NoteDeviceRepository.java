package com.newtech.note.repositories;

import com.newtech.note.entity.dto.NoteDevice;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface NoteDeviceRepository extends R2dbcRepository<NoteDevice, Long> {

    //默认继承了一堆CRUD方法； 像mybatis-plus

    //QBC： Query By Criteria
    //QBE： Query By Example

    //成为一个起名工程师  where id In () and name like ?
    //仅限单表复杂条件查询
    Flux<NoteDevice> findAllByIdInAndDeviceNameLike(List<Long> list, String name);

    /**
     * 支持多表查询
     *
     * @return
     */
    @Query("select * from note_device")
    Flux<NoteDevice> findCustomized();
}
