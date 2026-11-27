package com.bookmind.config;

import com.bookmind.entity.LoaiSach;
import com.bookmind.entity.Sach;
import com.bookmind.repository.LoaiSachRepository;
import com.bookmind.repository.SachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Order(2)
public class AdditionalCategoriesInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdditionalCategoriesInitializer.class);

    private final LoaiSachRepository loaiSachRepository;
    private final SachRepository sachRepository;

    public AdditionalCategoriesInitializer(LoaiSachRepository loaiSachRepository, SachRepository sachRepository) {
        this.loaiSachRepository = loaiSachRepository;
        this.sachRepository = sachRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Chỉ khởi tạo dữ liệu mẫu lần đầu nếu trong CSDL chưa đủ danh mục mẫu,
        // tuyệt đối không ghi đè dữ liệu mà người dùng đã chỉnh sửa hoặc xóa trên Web.
        if (loaiSachRepository.count() >= 9) {
            return;
        }
        initCategories();
        initBooks();
        log.info("Additional 5 categories and corresponding books initialized for the first time!");
    }

    private void initCategories() {
        updateOrCreateCategory(5L, "L\u1ECBch s\u1EED & Tri\u1EBFt h\u1ECDc", "C\u00E1c t\u00E1c ph\u1EA9m nghi\u00EAn c\u1EE9u l\u1ECBch s\u1EED th\u1EBF gi\u1EDBi, Vi\u1EC7t Nam v\u00E0 t\u01B0 t\u01B0\u1EDFng tri\u1EBFt h\u1ECDc tinh hoa");
        updateOrCreateCategory(6L, "Khoa h\u1ECDc & V\u0169 tr\u1EE5", "Kh\u00E1m ph\u00E1 b\u00ED \u1EA9n v\u0169 tr\u1EE5, thi\u00EAn v\u0103n h\u1ECDc, v\u1EADt l\u00FD v\u00E0 khoa h\u1ECDc t\u1EF1 nhi\u00EAn hi\u1EC7n \u0111\u1EA1i");
        updateOrCreateCategory(7L, "Thi\u1EBFu nhi & Tu\u1ED5i th\u01A1", "V\u0103n h\u1ECDc thi\u1EBFu nhi kinh \u0111i\u1EC3n, s\u00E1ch tranh nu\u00F4i d\u01B0\u1EE1ng t\u00E2m h\u1ED3n v\u00E0 tr\u00ED t\u01B0\u1EDFng t\u01B0\u1EE3ng");
        updateOrCreateCategory(8L, "Trinh th\u00E1m & B\u00ED \u1EA9n", "Ti\u1EC3u thuy\u1EBFt trinh th\u00E1m h\u1ED3i h\u1ED9p, ph\u00E1 \u00E1n gay c\u1EA5n v\u00E0 c\u00E1c c\u00E2u chuy\u1EC7n b\u00ED \u1EA9n kinh \u0111i\u1EC3n");
        updateOrCreateCategory(9L, "Nu\u00F4i d\u1EA1y con & Gia \u0111\u00ECnh", "Ph\u01B0\u01A1ng ph\u00E1p gi\u00E1o d\u1EE5c con th\u00F4ng th\u00E1i, ngh\u1EC7 thu\u1EADt l\u00E0m cha m\u1EB9 v\u00E0 gi\u1EEF g\u00ECn t\u1ED5 \u1EA5m gia \u0111\u00ECnh");
    }

    private LoaiSach updateOrCreateCategory(Long id, String tenLoai, String moTa) {
        return loaiSachRepository.findById(id).orElseGet(() -> {
            LoaiSach cat = new LoaiSach();
            cat.setTenLoai(tenLoai);
            cat.setMoTa(moTa);
            cat.setTrangThai("HOAT_DONG");
            return loaiSachRepository.save(cat);
        });
    }

    private void initBooks() {
        LoaiSach cat5 = loaiSachRepository.findById(5L).orElse(null);
        LoaiSach cat6 = loaiSachRepository.findById(6L).orElse(null);
        LoaiSach cat7 = loaiSachRepository.findById(7L).orElse(null);
        LoaiSach cat8 = loaiSachRepository.findById(8L).orElse(null);
        LoaiSach cat9 = loaiSachRepository.findById(9L).orElse(null);

        if (cat5 != null) {
            saveOrUpdateBook(206L, cat5, "Sapiens: Lược Sử Loài Người", "Yuval Noah Harari", "978-604-77-5001-1", 65,
                    "Cuốn sách đưa người đọc vào một chuyến hành trình kinh ngạc qua toàn bộ lịch sử loài người từ thời đồ đá đến kỷ nguyên AI.",
                    new BigDecimal("189000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Tri Thức", 2021, 560);
            saveOrUpdateBook(207L, cat5, "Homo Deus: Lược Sử Tương Lai", "Yuval Noah Harari", "978-604-77-5002-8", 50,
                    "Tác phẩm phác thảo viễn cảnh tương lai khi con người làm chủ công nghệ sinh học và trí tuệ nhân tạo để hướng tới sự trường sinh.",
                    new BigDecimal("199000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Thế Giới", 2021, 520);
            saveOrUpdateBook(208L, cat5, "21 Bài Học Cho Thế Kỷ 21", "Yuval Noah Harari", "978-604-77-5003-5", 45,
                    "Tổng kết những vấn đề cấp bách nhất của nhân loại đương đại: sự thật, tôn giáo, chiến tranh công nghệ và giáo dục tương lai.",
                    new BigDecimal("175000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Thế Giới", 2022, 440);
            saveOrUpdateBook(209L, cat5, "Súng, Vi Trùng Và Thép (Guns, Germs, and Steel)", "Jared Diamond", "978-604-77-5004-2", 40,
                    "Tác phẩm đoạt giải Pulitzer lý giải nguồn gốc sự thịnh vượng và suy tàn của các nền văn minh nhân loại qua địa lý và sinh thái.",
                    new BigDecimal("215000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Tri Thức", 2020, 680);
            saveOrUpdateBook(210L, cat5, "Sụp Đổ (Collapse)", "Jared Diamond", "978-604-77-5005-9", 35,
                    "Những bài học sống còn từ sự sụp đổ hoặc thành công của các xã hội loài người trong lịch sử trước biến đổi môi trường.",
                    new BigDecimal("205000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Tri Thức", 2021, 620);
            saveOrUpdateBook(211L, cat5, "Đại Việt Sử Ký Toàn Thư", "Ngô Sĩ Liên", "978-604-77-5006-6", 30,
                    "Bộ quốc sử vĩ đại ghi chép toàn diện lịch sử dân tộc Việt Nam từ thời Hồng Bàng đến thế kỷ XVII.",
                    new BigDecimal("350000"), "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "NXB Văn Học", 2020, 1200);
            saveOrUpdateBook(212L, cat5, "Việt Nam Sử Lược", "Trần Trọng Kim", "978-604-77-5007-3", 55,
                    "Cuốn sử lược đầu tiên viết bằng chữ quốc ngữ với lối văn trong sáng, khách quan, súc tích và chân thực.",
                    new BigDecimal("145000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Văn Học", 2021, 510);
            saveOrUpdateBook(213L, cat5, "Lịch Sử Văn Minh Thế Giới", "Will Durant", "978-604-77-5008-0", 25,
                    "Kiệt tác đồ sộ về các nền văn minh phương Đông và phương Tây, thành tựu văn hóa, nghệ thuật và tư tưởng.",
                    new BigDecimal("280000"), "https://images.unsplash.com/photo-1507842229451-7f01be8610ce?w=500", "NXB Hồng Đức", 2022, 750);
            saveOrUpdateBook(214L, cat5, "Thế Giới Của Sophie", "Jostein Gaarder", "978-604-77-5009-7", 70,
                    "Tiểu thuyết triết học lừng danh mở ra cánh cửa dẫn dắt người đọc khám phá toàn bộ dòng chảy tư tưởng phương Tây.",
                    new BigDecimal("165000"), "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "NXB Thế Giới", 2021, 560);
            saveOrUpdateBook(215L, cat5, "Luận Về Tự Do (On Liberty)", "John Stuart Mill", "978-604-77-5010-3", 40,
                    "Tác phẩm kinh điển về quyền tự do cá nhân, giới hạn quyền lực của nhà nước và xã hội đối với con người.",
                    new BigDecimal("95000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Tri Thức", 2020, 240);
            saveOrUpdateBook(216L, cat5, "Bàn Về Khế Ước Xã Hội", "Jean-Jacques Rousseau", "978-604-77-5011-0", 45,
                    "Nền tảng của tư tưởng chính trị hiện đại về chủ quyền nhân dân, tự do và bình đẳng pháp lý.",
                    new BigDecimal("110000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Thế Giới", 2021, 280);
            saveOrUpdateBook(217L, cat5, "Cộng Hòa (The Republic)", "Plato", "978-604-77-5012-7", 35,
                    "Tác phẩm vĩ đại nhất của triết học Hy Lạp cổ đại bàn về công lý, nhà nước lý tưởng và phẩm hạnh con người.",
                    new BigDecimal("195000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Thế Giới", 2022, 600);
            saveOrUpdateBook(218L, cat5, "Đạo Đức Kinh", "Lão Tử", "978-604-77-5013-4", 60,
                    "Trước tác tinh hoa của tư tưởng Đạo gia phương Đông với triết lý Vô vi, sống hòa hợp cùng tự nhiên.",
                    new BigDecimal("85000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Văn Học", 2021, 220);
        }

        if (cat6 != null) {
            saveOrUpdateBook(219L, cat6, "Vũ Trụ (Cosmos)", "Carl Sagan", "978-604-77-6001-0", 60,
                    "Bản thiên hùng ca tuyệt mỹ về 15 tỷ năm tiến hóa vũ trụ và sự phát triển của nền văn minh nhân loại.",
                    new BigDecimal("195000"), "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "NXB Thế Giới", 2021, 480);
            saveOrUpdateBook(220L, cat6, "Lược Sử Thời Gian", "Stephen Hawking", "978-604-77-6002-7", 75,
                    "Cuốn sách khoa học bán chạy nhất mọi thời đại giải thích các bí ẩn về Vụ Nổ Lớn, Lỗ Đen và Bản chất Thời gian.",
                    new BigDecimal("135000"), "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500", "NXB Trẻ", 2020, 320);
            saveOrUpdateBook(221L, cat6, "Vũ Trụ Trong Vỏ Hạt Dẻ", "Stephen Hawking", "978-604-77-6003-4", 50,
                    "Khám phá biên giới của vật lý lý thuyết: thuyết tương đối tổng quát, cơ học lượng tử và lý thuyết màng không gian đa chiều.",
                    new BigDecimal("155000"), "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=500", "NXB Trẻ", 2021, 280);
            saveOrUpdateBook(222L, cat6, "Nguồn Gốc Các Loài", "Charles Darwin", "978-604-77-6004-1", 40,
                    "Tác phẩm kinh điển đặt nền móng cho sinh học tiến hóa hiện đại thông qua cơ chế chọn lọc tự nhiên.",
                    new BigDecimal("220000"), "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500", "NXB Tri Thức", 2020, 640);
            saveOrUpdateBook(223L, cat6, "Giai Điệu Bí Ẩn", "Trịnh Xuân Thuận", "978-604-77-6005-8", 45,
                    "Vũ trụ học đại chúng được chắp bút bởi nhà vật lý thiên văn Việt kiều lừng danh với ngôn từ giàu chất thơ.",
                    new BigDecimal("140000"), "https://images.unsplash.com/photo-1507499739999-097706ad8914?w=500", "NXB Kim Đồng", 2021, 390);
            saveOrUpdateBook(224L, cat6, "Hỗn Độn Và Hài Hòa", "Trịnh Xuân Thuận", "978-604-77-6006-5", 40,
                    "Khám phá sự cân bằng kỳ diệu giữa tính ngẫu nhiên và trật tự tuyệt đối trong toàn cõi vũ trụ bao la.",
                    new BigDecimal("160000"), "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "NXB Tri Thức", 2020, 480);
            saveOrUpdateBook(225L, cat6, "Nguồn Gốc: Nỗi Hoài Niệm Về Những Thuở Ban Đầu", "Trịnh Xuân Thuận", "978-604-77-6007-2", 35,
                    "Hành trình truy tầm cội nguồn nguyên thủy của các vì sao, dải thiên hà và mầm sống đầu tiên trên Trái Đất.",
                    new BigDecimal("175000"), "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500", "NXB Tri Thức", 2022, 510);
            saveOrUpdateBook(226L, cat6, "Thế Giới Như Tôi Thấy", "Albert Einstein", "978-604-77-6008-9", 55,
                    "Góc nhìn sâu sắc của thiên tài vật lý Albert Einstein về khoa học, tôn giáo, đạo đức, hòa bình và ý nghĩa cuộc sống.",
                    new BigDecimal("98000"), "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=500", "NXB Tri Thức", 2021, 240);
            saveOrUpdateBook(227L, cat6, "Chết Bởi Lỗ Đen (Death by Black Hole)", "Neil deGrasse Tyson", "978-604-77-6009-6", 50,
                    "Những điều kỳ lạ và thú vị nhất nơi không gian vũ trụ được diễn giải dí dỏm, cuốn hút và dễ hiểu.",
                    new BigDecimal("145000"), "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=500", "NXB Thế Giới", 2021, 380);
            saveOrUpdateBook(228L, cat6, "Tương Lai Nhân Loại (The Future of Humanity)", "Michio Kaku", "978-604-77-6010-2", 45,
                    "Chinh phục sao Hỏa, du hành liên sao, sự bất tử và định cư ngoài không gian của nền văn minh tương lai.",
                    new BigDecimal("185000"), "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "NXB Thế Giới", 2022, 450);
            saveOrUpdateBook(229L, cat6, "Các Thế Giới Song Song (Parallel Worlds)", "Michio Kaku", "978-604-77-6011-9", 40,
                    "Hành trình vượt qua không gian đa chiều, thuyết dây và tương lai của vũ trụ theo góc nhìn vật lý hiện đại.",
                    new BigDecimal("175000"), "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500", "NXB Thế Giới", 2021, 460);
            saveOrUpdateBook(230L, cat6, "Bản Thiết Kế Vĩ Đại (The Grand Design)", "Stephen Hawking", "978-604-77-6012-6", 50,
                    "Hawking và Leonard Mlodinow trình bày những quan điểm đột phá nhất về Lý thuyết M - ứng viên hàng đầu cho Thuyết vạn vật.",
                    new BigDecimal("125000"), "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=500", "NXB Trẻ", 2020, 260);
            saveOrUpdateBook(231L, cat6, "Gen: Lịch Sử Và Tương Lai Của Nhân Loại", "Siddhartha Mukherjee", "978-604-77-6013-3", 35,
                    "Tác phẩm xuất chúng về lịch sử di truyền học, mã di truyền con người và câu hỏi đạo đức khi chỉnh sửa bộ gen.",
                    new BigDecimal("245000"), "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500", "NXB Dân Trí", 2022, 710);
        }

        if (cat7 != null) {
            saveOrUpdateBook(232L, cat7, "Hoàng Tử Bé (Le Petit Prince)", "Antoine de Saint-Exupéry", "978-604-77-7001-9", 100,
                    "Tác phẩm bất hủ về tình yêu thương, tình bạn thuần khiết và bài học nhìn thế giới bằng trái tim.",
                    new BigDecimal("75000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Hội Nhà Văn", 2022, 120);
            saveOrUpdateBook(233L, cat7, "Chuyện Con Mèo Dạy Hải Âu Bay", "Luis Sepúlveda", "978-604-77-7002-6", 90,
                    "Câu chuyện ấm áp, cảm động về tình mẫu tử vượt qua mọi giống loài giữa chú mèo mun mập ú Zorba và cô chim hải âu bé nhỏ.",
                    new BigDecimal("65000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Hội Nhà Văn", 2021, 144);
            saveOrUpdateBook(234L, cat7, "Pippi Tất Dài (Pippi Longstocking)", "Astrid Lindgren", "978-604-77-7003-3", 60,
                    "Cô bé khỏe nhất thế giới với tính cách hào phóng, hài hước và những cuộc phiêu lưu đầy ắp tiếng cười tuổi thơ.",
                    new BigDecimal("85000"), "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "NXB Văn Học", 2021, 230);
            saveOrUpdateBook(235L, cat7, "Chiếc Hộp Pandora", "Nhiều Tác Giả", "978-604-77-7004-0", 50,
                    "Tuyển tập những câu chuyện thần thoại Hy Lạp kỳ ảo, hấp dẫn nuôi dưỡng trí tưởng tượng phong phú của trẻ.",
                    new BigDecimal("95000"), "https://images.unsplash.com/photo-1507842229451-7f01be8610ce?w=500", "NXB Kim Đồng", 2022, 280);
            saveOrUpdateBook(236L, cat7, "Những Cuộc Phiêu Lưu Của Tom Sawyer", "Mark Twain", "978-604-77-7005-7", 70,
                    "Tuổi thơ tinh nghịch, tự do bên dòng sông Mississippi của cậu bé Tom Sawyer thông minh và can đảm.",
                    new BigDecimal("90000"), "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "NXB Văn Học", 2020, 310);
            saveOrUpdateBook(237L, cat7, "Anne Tóc Đỏ Dưới Mái Nhà Xanh", "L. M. Montgomery", "978-604-77-7006-4", 65,
                    "Cô bé mồ côi Anne tóc đỏ tràn đầy năng lượng và tình yêu cuộc sống đã sưởi ấm tâm hồn của cả hòn đảo hoàng tử Edward.",
                    new BigDecimal("115000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Hội Nhà Văn", 2021, 420);
            saveOrUpdateBook(238L, cat7, "Charlie Và Nhà Máy Sôcôla", "Roald Dahl", "978-604-77-7007-1", 75,
                    "Chuyến tham quan kỳ diệu bên trong nhà máy kẹo sôcôla bí ẩn của ngài Willy Wonka lập dị.",
                    new BigDecimal("80000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Kim Đồng", 2022, 200);
            saveOrUpdateBook(239L, cat7, "Matilda", "Roald Dahl", "978-604-77-7008-8", 60,
                    "Cô bé thiên tài Matilda yêu sách và sở hữu năng lực đặc biệt đã dũng cảm bảo vệ cô giáo hiền lành trước hiệu trưởng hung ác.",
                    new BigDecimal("82000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Kim Đồng", 2021, 240);
            saveOrUpdateBook(240L, cat7, "Gió Qua Rặng Liễu", "Kenneth Grahame", "978-604-77-7009-5", 45,
                    "Kiệt tác văn học thiếu nhi kinh điển của Anh với tình bạn ấm áp giữa Chuột Chũi, Chuột Nước và Bác Cóc.",
                    new BigDecimal("88000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Hội Nhà Văn", 2020, 260);
            saveOrUpdateBook(241L, cat7, "Peter Pan", "J. M. Barrie", "978-604-77-7010-1", 70,
                    "Cậu bé không bao giờ lớn và chuyến phiêu lưu diệu kỳ tới vùng đất Neverland cùng cô bé Wendy.",
                    new BigDecimal("78000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Văn Học", 2021, 210);
            saveOrUpdateBook(242L, cat7, "Alice Ở Xứ Sở Thần Tiên", "Lewis Carroll", "978-604-77-7011-8", 65,
                    "Cuộc hành trình kỳ ảo của cô bé Alice chui qua hang thỏ bước vào thế giới phép thuật kỳ lạ và đầy bất ngờ.",
                    new BigDecimal("85000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Hội Nhà Văn", 2022, 190);
            saveOrUpdateBook(243L, cat7, "Khu Vườn Bí Mật (The Secret Garden)", "Frances Hodgson Burnett", "978-604-77-7012-5", 55,
                    "Kỳ quan văn học cảm động về sự hồi sinh tâm hồn và thể xác của những đứa trẻ nhờ vào tình yêu thiên nhiên.",
                    new BigDecimal("95000"), "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "NXB Hội Nhà Văn", 2021, 350);
            saveOrUpdateBook(244L, cat7, "Chú Bé Mang Pajama Sọc", "John Boyne", "978-604-77-7013-2", 50,
                    "Tình bạn trong sáng, thuần khiết xuyên qua hàng rào dây thép gai giữa hai cậu bé trong bối cảnh Thế chiến II.",
                    new BigDecimal("85000"), "https://images.unsplash.com/photo-1507842229451-7f01be8610ce?w=500", "NXB Hội Nhà Văn", 2020, 230);
        }

        if (cat8 != null) {
            saveOrUpdateBook(245L, cat8, "Án Mạng Trên Chuyến Tàu Tốc Hành Phương Đông", "Agatha Christie", "978-604-77-8001-8", 80,
                    "Vụ án bí ẩn trên chuyến tàu tuyết phủ xuyên châu Âu với sự phá án kinh điển của thám tử Hercule Poirot.",
                    new BigDecimal("110000"), "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "NXB Trẻ", 2021, 320);
            saveOrUpdateBook(246L, cat8, "Mười Người Da Đen Nhỏ (And Then There Were None)", "Agatha Christie", "978-604-77-8002-5", 90,
                    "Kiệt tác trinh thám kinh dị bán chạy nhất mọi thời đại: mười vị khách bị cô lập trên hòn đảo và lần lượt bỏ mạng.",
                    new BigDecimal("115000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Trẻ", 2021, 300);
            saveOrUpdateBook(247L, cat8, "Phía Sau Nghi Can X", "Keigo Higashino", "978-604-77-8003-2", 95,
                    "Cuộc đối đầu trí tuệ đỉnh cao giữa thiên tài toán học Ishigami và nhà vật lý Yukawa vì một tình yêu thầm kín.",
                    new BigDecimal("108000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Hội Nhà Văn", 2022, 390);
            saveOrUpdateBook(248L, cat8, "Bạch Dạ Hành (Journey Under the Midnight Sun)", "Keigo Higashino", "978-604-77-8004-9", 85,
                    "Bản trường ca bi tráng, u tối và ám ảnh bậc nhất về hai số phận nương tựa nhau bước đi dưới ánh mặt trời đêm trắng.",
                    new BigDecimal("175000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Hội Nhà Văn", 2021, 620);
            saveOrUpdateBook(249L, cat8, "Sự Cứu Rỗi Của Kẻ Tình Nghi X", "Keigo Higashino", "978-604-77-8005-6", 60,
                    "Những góc khuất tâm lý tội phạm và nút thắt bất ngờ làm nên tên tuổi của bậc thầy trinh thám Nhật Bản.",
                    new BigDecimal("120000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Hội Nhà Văn", 2020, 360);
            saveOrUpdateBook(250L, cat8, "Bí Mật Của Naoko", "Keigo Higashino", "978-604-77-8006-3", 70,
                    "Tấn bi kịch gia đình xót xa khi linh hồn người vợ nhập vào thể xác con gái sau một tai nạn thảm khốc.",
                    new BigDecimal("115000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Hội Nhà Văn", 2021, 380);
            saveOrUpdateBook(251L, cat8, "Sherlock Holmes Toàn Tập (Tập 1)", "Arthur Conan Doyle", "978-604-77-8007-0", 75,
                    "Tượng đài trinh thám thế giới với những vụ án kinh điển của thám tử đại tài Sherlock Holmes và bác sĩ Watson.",
                    new BigDecimal("165000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Văn Học", 2021, 580);
            saveOrUpdateBook(252L, cat8, "Thú Tội (Confessions)", "Minato Kanae", "978-604-77-8008-7", 65,
                    "Lời thú tội rợn người của nữ giáo viên mầm non trả thù hai học sinh vị thành niên đã sát hại con gái mình.",
                    new BigDecimal("99000"), "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "NXB Hội Nhà Văn", 2022, 260);
            saveOrUpdateBook(253L, cat8, "Chiếc Thuyền Lông Vịt", "Minato Kanae", "978-604-77-8009-4", 50,
                    "Tác phẩm trinh thám tâm lý xoay quanh những góc khuất trong gia đình và bi kịch của sự dối trá.",
                    new BigDecimal("95000"), "https://images.unsplash.com/photo-1507842229451-7f01be8610ce?w=500", "NXB Hội Nhà Văn", 2021, 280);
            saveOrUpdateBook(254L, cat8, "Mật Mã Da Vinci (The Da Vinci Code)", "Dan Brown", "978-604-77-8010-0", 80,
                    "Cuộc săn tìm Chén Thánh nghẹt thở với các mật mã ẩn giấu trong những kiệt tác hội họa của Leonardo da Vinci.",
                    new BigDecimal("155000"), "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "NXB Lao Động", 2020, 560);
            saveOrUpdateBook(255L, cat8, "Thiên Thần Và Ác Quỷ (Angels & Demons)", "Dan Brown", "978-604-77-8011-7", 75,
                    "Giáo sư Robert Langdon chạy đua cùng thời gian tại Vatican để ngăn chặn âm mưu hủy diệt của hội kín Illuminati.",
                    new BigDecimal("160000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Lao Động", 2021, 590);
            saveOrUpdateBook(256L, cat8, "Hỏa Ngục (Inferno)", "Dan Brown", "978-604-77-8012-4", 65,
                    "Bí ẩn thần khúc Dante và âm mưu phát tán virus diệt vong để cứu rỗi Trái Đất trước khủng hoảng dân số.",
                    new BigDecimal("165000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Lao Động", 2022, 600);
            saveOrUpdateBook(257L, cat8, "Án Mạng Mười Một Chữ", "Keigo Higashino", "978-604-77-8013-1", 60,
                    "Bức thông điệp bí ẩn bắt đầu cho chuỗi án mạng liên hoàn nhằm vào các thành viên một câu lạc bộ du thuyền.",
                    new BigDecimal("105000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Hội Nhà Văn", 2021, 340);
        }

        if (cat9 != null) {
            saveOrUpdateBook(258L, cat9, "Yêu Thương Không Đặt Điều Kiện", "Alfie Kohn", "978-604-77-9001-7", 60,
                    "Phương pháp nuôi dạy con nhân văn, từ bỏ các hình thức thưởng phạt áp đặt để bồi dưỡng nhân cách tự chủ cho trẻ.",
                    new BigDecimal("135000"), "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500", "NXB Thế Giới", 2021, 380);
            saveOrUpdateBook(259L, cat9, "Cha Mẹ Tỉnh Thức", "Shefali Tsabary", "978-604-77-9002-4", 70,
                    "Hành trình thay đổi chính bản thân cha mẹ để kết nối sâu sắc, chữa lành và tôn trọng cá tính riêng của con.",
                    new BigDecimal("145000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Thế Giới", 2022, 420);
            saveOrUpdateBook(260L, cat9, "Nuôi Con Không Phải Là Cuộc Chiến", "Hachun Lyonnet & Bubu Huong", "978-604-77-9003-1", 90,
                    "Cẩm nang rèn nếp sinh hoạt EASY, ăn dặm tự chỉ huy và tự ngủ chuẩn khoa học cho các gia đình Việt.",
                    new BigDecimal("160000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Lao Động", 2021, 360);
            saveOrUpdateBook(261L, cat9, "Phương Pháp Giáo Dục Con Của Người Do Thái", "Sara Imas", "978-604-77-9004-8", 85,
                    "Triết lý dạy con tự lập sớm, bồi dưỡng chỉ số vượt khó AQ và kỹ năng quản lý tài chính từ nhỏ của người Do Thái.",
                    new BigDecimal("125000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Hồng Đức", 2021, 390);
            saveOrUpdateBook(262L, cat9, "Nói Sao Cho Con Nghe Lời", "Adele Faber & Elaine Mazlish", "978-604-77-9005-5", 75,
                    "Nghệ thuật giao tiếp tích cực giữa cha mẹ và con cái, giải tỏa cơn giận dữ và khuyến khích con hợp tác.",
                    new BigDecimal("115000"), "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500", "NXB Phụ Nữ", 2020, 310);
            saveOrUpdateBook(263L, cat9, "Để Con Được Tự Do", "Maria Montessori", "978-604-77-9006-2", 65,
                    "Nguyên lý giáo dục sớm tôn trọng sự phát triển tự nhiên của trẻ theo triết lý Montessori trứ danh.",
                    new BigDecimal("110000"), "https://images.unsplash.com/photo-1507842229451-7f01be8610ce?w=500", "NXB Dân Trí", 2021, 290);
            saveOrUpdateBook(264L, cat9, "Kỷ Luật Không Nước Mắt", "Peter L. Stavinoha", "978-604-77-9007-9", 55,
                    "Xây dựng thói quen tốt và nguyên tắc ứng xử cho con mà không cần đòn roi hay la mắng tổn thương.",
                    new BigDecimal("105000"), "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=500", "NXB Thế Giới", 2022, 270);
            saveOrUpdateBook(265L, cat9, "Vô Cùng Tàn Nhẫn Vô Cùng Yêu Thương", "Sara Imas", "978-604-77-9008-6", 80,
                    "Tình yêu thương con đúng đắn: buông tay cho con tự lập, trui rèn bản lĩnh để con trưởng thành vững vàng.",
                    new BigDecimal("130000"), "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500", "NXB Lao Động", 2020, 450);
            saveOrUpdateBook(266L, cat9, "Con Nghĩ Đi, Mẹ Không Biết!", "Thu Hà", "978-604-77-9009-3", 70,
                    "Những câu chuyện đời thường giản dị, chân thành giúp con rèn luyện tư duy độc lập và giải quyết vấn đề.",
                    new BigDecimal("95000"), "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500", "NXB Trẻ", 2021, 260);
            saveOrUpdateBook(267L, cat9, "Chờ Đến Mẫu Giáo Thì Đã Muộn", "Ibuka Masaru", "978-604-77-9010-9", 95,
                    "Khai mở tiềm năng trí tuệ của trẻ trong giai đoạn vàng từ 0 đến 3 tuổi của nhà sáng lập tập đoàn Sony.",
                    new BigDecimal("85000"), "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=500", "NXB Lao Động", 2020, 210);
            saveOrUpdateBook(268L, cat9, "Người Mẹ Tốt Hơn Người Thầy Tốt", "Doãn Kiến Lợi", "978-604-77-9011-6", 60,
                    "Phương pháp giáo dục gia đình thực tiễn, gieo mầm niềm yêu thích đọc sách và hình thành nhân cách tốt đẹp.",
                    new BigDecimal("140000"), "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=500", "NXB Văn Học", 2021, 480);
            saveOrUpdateBook(269L, cat9, "Đọc Vị Mọi Vấn Đề Của Trẻ", "Tracy Hogg & Melinda Blau", "978-604-77-9012-3", 50,
                    "Thấu hiểu tiếng khóc, giấc ngủ, chế độ ăn và tâm lý của trẻ sơ sinh và trẻ nhỏ một cách khoa học.",
                    new BigDecimal("155000"), "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500", "NXB Thế Giới", 2022, 490);
            saveOrUpdateBook(270L, cat9, "Cha Mẹ Tỉnh Thức Trong Thế Giới Số", "Jon Kabat-Zinn", "978-604-77-9013-0", 45,
                    "Giúp con cân bằng giữa đời sống thực tế và thế giới mạng, xây dựng lối sống lành mạnh cho cả gia đình.",
                    new BigDecimal("125000"), "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500", "NXB Tri Thức", 2021, 330);
        }
    }

    private void saveOrUpdateBook(Long id, LoaiSach loai, String tenSach, String tacGia, String isbn,
                                  int soLuong, String moTa, BigDecimal giaBan, String hinhAnh,
                                  String nxb, int namXb, int soTrang) {
        if (sachRepository.existsById(id)) {
            return;
        }
        Sach book = new Sach();
        book.setLoaiSach(loai);
        book.setTenSach(tenSach);
        book.setTacGia(tacGia);
        book.setIsbn(isbn);
        book.setSoLuong(soLuong);
        book.setMoTa(moTa);
        book.setGiaBan(giaBan);
        book.setHinhAnh(hinhAnh);
        book.setNxb(nxb);
        book.setNamXb(namXb);
        book.setSoTrang(soTrang);
        book.setTrangThai("DANG_BAN");
        sachRepository.save(book);
    }
}
