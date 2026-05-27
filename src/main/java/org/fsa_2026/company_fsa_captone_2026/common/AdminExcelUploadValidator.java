package org.fsa_2026.company_fsa_captone_2026.common;

import org.fsa_2026.company_fsa_captone_2026.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Validate file upload Excel cho Admin (levels, users, ...).
 * Fix A-03, A-04, A-06: chặn file rỗng và sai định dạng trước khi POI parse (tránh 500).
 */
public final class AdminExcelUploadValidator {

    private static final String MSG_EMPTY = "File upload không được để trống";
    private static final String MSG_FORMAT = "Chỉ chấp nhận file Excel định dạng .xlsx";

    private AdminExcelUploadValidator() {
    }

    /**
     * Kiểm tra multipart trước import: không rỗng, đuôi .xlsx, content-type hợp lệ (nếu có).
     */
    public static void validateXlsxUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(MSG_EMPTY);
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BadRequestException(MSG_FORMAT);
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            boolean allowed = normalized.contains("spreadsheet")
                    || normalized.contains("excel")
                    || normalized.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || normalized.equals("application/octet-stream");
            if (!allowed) {
                throw new BadRequestException(MSG_FORMAT);
            }
        }
    }
}
