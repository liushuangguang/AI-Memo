package com.newtech.note.config.converter;

import com.newtech.note.entity.dto.NoteDevice;
import com.newtech.note.entity.vo.NoteAnalysisDeviceVo;
import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

/**
 * 数据读取转换器
 */
@ReadingConverter
public class NoteAnalysisDeviceVoConverter implements Converter<Row, NoteAnalysisDeviceVo> {

    /**
     * <ol>
     * <li>@Query 指定了 sql如何发送</li>
     * <li>自定义 NoteAnalysisDeviceVoConverter 指定了 数据库返回的一 Row 数据，怎么封装成
     * NoteAnalysisDeviceVo</li>
     * <li>配置 R2dbcCustomConversions 组件，让 NoteAnalysisDeviceVoConverter 加入其中生效</li>
     * </ol>
     *
     * @param source each row data from database
     * @return NoteAnalysisDeviceVo
     */
    @Override
    public NoteAnalysisDeviceVo convert(@NonNull Row source) {
        NoteAnalysisDeviceVo noteDeviceVo = new NoteAnalysisDeviceVo();
        noteDeviceVo.setId(source.get("id", Long.class));
        noteDeviceVo.setTitle(source.get("title", String.class));

        noteDeviceVo.setDeviceId(source.get("device_id", String.class));
        noteDeviceVo.setNoteId(source.get("note_id", String.class));
        noteDeviceVo.setNoteAnalysisContent(source.get("note_analysis_content", String.class));
        noteDeviceVo.setRawNote(source.get("raw_note", String.class));
        noteDeviceVo.setCreatedAt(source.get("created_at", LocalDateTime.class));
        noteDeviceVo.setUpdatedAt(source.get("updated_at", LocalDateTime.class));

        // 让 converter兼容更多的表结构处理
        if (source.getMetadata().contains("device_id")) {
            NoteDevice noteDevice = new NoteDevice();
            noteDevice.setId(noteDevice.getId());
            noteDevice.setDeviceId(source.get("device_id", String.class));
            noteDevice.setDeviceName(source.get("device_name", String.class));
            noteDevice.setCreatedAt(source.get("device_created_at", LocalDateTime.class));
            noteDevice.setUpdatedAt(source.get("device_updated_at", LocalDateTime.class));
            noteDeviceVo.setNoteDevice(noteDevice);
        }
        return noteDeviceVo;
    }
}
