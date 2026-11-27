package com.bookmind;

import com.bookmind.dto.LoaiSachFormDto;
import com.bookmind.entity.LoaiSach;
import com.bookmind.repository.LoaiSachRepository;
import com.bookmind.repository.SachRepository;
import com.bookmind.service.LoaiSachService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test độc lập dùng JUnit 5 + Mockito (không phụ thuộc MySQL)
 * cho phân hệ Quản lý Loại sách (LoaiSachService).
 */
@ExtendWith(MockitoExtension.class)
class LoaiSachServiceUnitTest {

    @Mock
    private LoaiSachRepository loaiSachRepository;

    @Mock
    private SachRepository sachRepository;

    @InjectMocks
    private LoaiSachService loaiSachService;

    @Test
    @DisplayName("Xóa loại sách: Chặn xóa và ném IllegalStateException khi loại sách đang có đầu sách liên kết")
    void delete_whenCategoryHasBooks_throwsIllegalStateException() {
        LoaiSach loai = new LoaiSach();
        loai.setMaLoai(1L);
        loai.setTenLoai("Văn học kinh điển");

        when(loaiSachRepository.findById(1L)).thenReturn(Optional.of(loai));
        when(sachRepository.countByLoaiSach_MaLoai(1L)).thenReturn(12L);

        assertThatThrownBy(() -> loaiSachService.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể xóa loại sách")
                .hasMessageContaining("12 đầu sách");

        verify(loaiSachRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Xóa loại sách: Xóa thành công khi số đầu sách thuộc thể loại bằng 0")
    void delete_whenCategoryHasNoBooks_deletesSuccessfully() {
        LoaiSach loai = new LoaiSach();
        loai.setMaLoai(2L);
        loai.setTenLoai("Thể loại thử nghiệm");

        when(loaiSachRepository.findById(2L)).thenReturn(Optional.of(loai));
        when(sachRepository.countByLoaiSach_MaLoai(2L)).thenReturn(0L);

        loaiSachService.delete(2L);

        verify(loaiSachRepository).delete(loai);
    }

    @Test
    @DisplayName("Bật/Tắt trạng thái loại sách: Chuyển đổi chuẩn xác giữa HOAT_DONG và NGUNG")
    void toggleStatus_switchesBetweenHoatDongAndNgung() {
        LoaiSach loai = new LoaiSach();
        loai.setMaLoai(3L);
        loai.setTenLoai("Kinh tế");
        loai.setTrangThai("HOAT_DONG");

        when(loaiSachRepository.findById(3L)).thenReturn(Optional.of(loai));

        loaiSachService.toggleStatus(3L);
        assertThat(loai.getTrangThai()).isEqualTo("NGUNG");

        loaiSachService.toggleStatus(3L);
        assertThat(loai.getTrangThai()).isEqualTo("HOAT_DONG");

        verify(loaiSachRepository, times(2)).save(loai);
    }

    @Test
    @DisplayName("Lưu loại sách mới: Gán đúng thông tin từ DTO và lưu qua Repository")
    void save_createsNewCategoryFromDto() {
        LoaiSachFormDto dto = new LoaiSachFormDto();
        dto.setTenLoai("Tâm lý - Kỹ năng");
        dto.setMoTa("Sách phát triển bản thân");
        dto.setTrangThai("HOAT_DONG");

        when(loaiSachRepository.save(any(LoaiSach.class))).thenAnswer(inv -> inv.getArgument(0));

        LoaiSach saved = loaiSachService.save(dto);

        assertThat(saved.getTenLoai()).isEqualTo("Tâm lý - Kỹ năng");
        assertThat(saved.getMoTa()).isEqualTo("Sách phát triển bản thân");
        assertThat(saved.getTrangThai()).isEqualTo("HOAT_DONG");
        verify(loaiSachRepository).save(any(LoaiSach.class));
    }
}
