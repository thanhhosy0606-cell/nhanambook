package com.bookmind.service;

import com.bookmind.dto.LoaiSachFormDto;
import com.bookmind.entity.LoaiSach;
import com.bookmind.repository.LoaiSachRepository;
import com.bookmind.repository.SachRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoaiSachService {

    private final LoaiSachRepository loaiSachRepository;
    private final SachRepository sachRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public LoaiSachService(LoaiSachRepository loaiSachRepository, SachRepository sachRepository) {
        this.loaiSachRepository = loaiSachRepository;
        this.sachRepository = sachRepository;
    }

    public List<LoaiSach> getAllActive() {
        return loaiSachRepository.findByTrangThai("HOAT_DONG");
    }

    public List<LoaiSach> getAllForAdmin() {
        return loaiSachRepository.findAll();
    }

    public List<LoaiSach> searchForAdmin(String keyword, String status) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String st = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) ? status.trim() : null;
        if (kw == null && st == null) {
            return loaiSachRepository.findAll();
        }
        return loaiSachRepository.searchCategories(kw, st);
    }

    public long getBookCount(Long maLoai) {
        return sachRepository.countByLoaiSach_MaLoai(maLoai);
    }

    public LoaiSach getById(Long id) {
        return loaiSachRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại sách: " + id));
    }

    @Transactional
    public LoaiSach save(LoaiSachFormDto dto) {
        LoaiSach loai;
        if (dto.getMaLoai() != null) {
            loai = getById(dto.getMaLoai());
        } else {
            loai = new LoaiSach();
        }
        loai.setTenLoai(dto.getTenLoai());
        loai.setHinhAnh(dto.getHinhAnh());
        loai.setMoTa(dto.getMoTa());
        if (dto.getTrangThai() != null) {
            loai.setTrangThai(dto.getTrangThai());
        }
        return loaiSachRepository.save(loai);
    }

    @Transactional
    public void toggleStatus(Long id) {
        LoaiSach loai = getById(id);
        if ("HOAT_DONG".equalsIgnoreCase(loai.getTrangThai())) {
            loai.setTrangThai("NGUNG");
        } else {
            loai.setTrangThai("HOAT_DONG");
        }
        loaiSachRepository.save(loai);
    }

    @Transactional
    public void delete(Long id) {
        LoaiSach loai = getById(id);
        long bookCount = sachRepository.countByLoaiSach_MaLoai(id);
        if (bookCount > 0) {
            throw new IllegalStateException("Không thể xóa loại sách '" + loai.getTenLoai() + "' vì đang có " + bookCount + " đầu sách thuộc thể loại này! Vui lòng chuyển các sách sang thể loại khác hoặc chuyển trạng thái sang 'Ngừng hoạt động'.");
        }
        loaiSachRepository.delete(loai);
    }
}
