package org.fsa_2026.company_fsa_captone_2026.common;

import org.fsa_2026.company_fsa_captone_2026.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Kiểm tra upload / tham chiếu âm thanh theo quy ước dự án: chỉ chấp nhận WebM ({@code .webm}).
 */
public final class WebmAudioValidator {

    private static final String WEBM_EXTENSION = ".webm";
    private static final String MSG_EMPTY = "File âm thanh không được để trống";
    private static final String MSG_FORMAT = "Chỉ chấp nhận file âm thanh định dạng .webm";
    private static final String MSG_URL_FORMAT = "URL âm thanh phải trỏ tới file .webm";

    private WebmAudioValidator() {
    }

    /**
     * Validate multipart audio trước khi gọi ASR / Groq / Firebase.
     *
     * @param audioFile file từ {@code multipart/form-data}
     * @throws BadRequestException nếu file null, rỗng, sai đuôi hoặc Content-Type không hợp lệ
     */
    public static void validateMultipart(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BadRequestException(MSG_EMPTY);
        }
        String filename = audioFile.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(WEBM_EXTENSION)) {
            throw new BadRequestException(MSG_FORMAT);
        }
        String contentType = audioFile.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            boolean allowedType = normalized.contains("webm")
                    || "application/octet-stream".equals(normalized);
            if (!allowedType) {
                throw new BadRequestException(MSG_FORMAT);
            }
        }
    }

    /**
     * Validate {@code audioUrl} khi client gửi URL file đã upload (endpoint feedback).
     *
     * @param audioUrl URL Firebase hoặc CDN; {@code null}/blank → bỏ qua (không bắt buộc)
     * @throws BadRequestException nếu có giá trị nhưng không chứa {@code .webm}
     */
    public static void validateAudioUrl(String audioUrl) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return;
        }
        String path = audioUrl.split("\\?")[0].toLowerCase(Locale.ROOT);
        if (!path.contains(WEBM_EXTENSION)) {
            throw new BadRequestException(MSG_URL_FORMAT);
        }
    }
}
