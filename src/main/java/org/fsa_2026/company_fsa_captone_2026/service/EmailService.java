package org.fsa_2026.company_fsa_captone_2026.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email Service
 * Gửi email thông qua SMTP (Gmail)
 * Hỗ trợ: xác thực email sau đăng ký, đặt lại mật khẩu
 *
 * LƯU Ý: Gmail SMTP chỉ cho phép gửi từ email đã xác thực.
 * Nên fromAddress PHẢI trùng với spring.mail.username
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:nguyenductuan11012003@gmail.com}")
    private String mailUsername;

    @Value("${mail.from:#{null}}")
    private String fromAddress;

    @Value("${mail.base-url:http://localhost:5173}")
    private String baseUrl;

    /**
     * Gửi email xác thực tài khoản với mã OTP sau khi đăng ký
     */
    @Async
    public void sendEmailVerification(String toEmail, String fullName, String otpCode) {
        String subject = "SpeakVN - Xác nhận địa chỉ email";
        String verifyLink = baseUrl + "/verify-email?email=" + toEmail + "&code=" + otpCode;
        String supportLink = baseUrl + "/support";
        String content = buildEmailVerificationHtml(fullName, verifyLink, supportLink, otpCode);
        sendHtmlEmail(toEmail, subject, content);
    }

    /**
     * Gửi email đặt lại mật khẩu với mã OTP
     */
    @Async
    public void sendResetPasswordEmail(String toEmail, String fullName, String resetCode) {
        String subject = "SpeakVN - Khôi phục mật khẩu";
        String resetLink = baseUrl + "/reset-password?email=" + toEmail + "&code=" + resetCode;
        String supportLink = baseUrl + "/support";
        String content = buildResetPasswordHtml(fullName, resetCode, resetLink, supportLink);
        sendHtmlEmail(toEmail, subject, content);
    }

    /**
     * Gửi thông báo tạo tài khoản Educator thành công với mật khẩu tự sinh
     */
    @Async
    public void sendEducatorAccountCreatedEmail(String toEmail, String fullName, String password) {
        String subject = "SpeakVN - Tài khoản Giáo viên của bạn đã được tạo";
        String loginLink = baseUrl + "/login";
        String supportLink = baseUrl + "/support";
        String content = buildEducatorAccountHtml(fullName, password, loginLink, supportLink);
        sendHtmlEmail(toEmail, subject, content);
    }

    /**
     * Gửi email HTML
     * Gmail SMTP yêu cầu from address phải trùng với tài khoản đã xác thực
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Gmail chỉ cho phép gửi từ email đã xác thực
            // Dùng InternetAddress để set tên hiển thị + email thật
            String senderEmail = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : mailUsername;
            helper.setFrom(new InternetAddress(senderEmail, "SpeakVN", "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Đã gửi email thành công đến: {}", to);
        } catch (MessagingException e) {
            log.error("❌ Gửi email thất bại (MessagingException) đến: {} - Lỗi: {}", to, e.getMessage(), e);
        } catch (MailException e) {
            log.error("❌ Gửi email thất bại (MailException) đến: {} - Lỗi: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Gửi email thất bại (Unexpected) đến: {} - Lỗi: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Template HTML - Xác nhận địa chỉ email (Duolingo-style)
     */
    private String buildEmailVerificationHtml(String userName, String verifyLink, String supportLink, String otpCode) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Xác nhận địa chỉ email</title>
                </head>
                <body style="font-family: 'Nunito', 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f7f9fa; margin: 0; padding: 0; color: #4b4b4b; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100%% !important;">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f9fa; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 20px; overflow: hidden; border: 2px solid #e5e5e5; text-align: center;">
                                    <tr>
                                        <td align="center" style="background-color: #1cb0f6; padding: 32px 20px;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: 0.5px; font-family: 'Nunito', Arial, sans-serif;">Chào mừng bạn!</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 40px 32px;">
                                            <h2 style="font-size: 22px; color: #4b4b4b; margin: 0 0 16px 0; font-weight: 800;">Xác nhận địa chỉ email</h2>
                                            <p style="font-size: 16px; line-height: 1.6; color: #777777; margin: 0 0 24px 0;">
                                                Xin chào <strong>%s</strong>,<br><br>
                                                Cảm ơn bạn đã tham gia cùng chúng tôi. Để hoàn tất việc đăng ký và bắt đầu trải nghiệm, vui lòng xác nhận địa chỉ email của bạn bằng mã OTP hoặc nhấn vào nút bên dưới.
                                            </p>
                                            <div style="margin: 24px 0;">
                                                <span style="font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #1cb0f6; background: #eef9ff; padding: 16px 32px; border-radius: 12px; border: 2px dashed #1cb0f6; display: inline-block;">
                                                    %s
                                                </span>
                                            </div>
                                            <p style="font-size: 14px; color: #ef4444; font-weight: 700; margin: 0 0 28px 0;">⏰ Mã này có hiệu lực trong 15 phút.</p>
                                            <table border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="border-radius: 16px; background-color: #58cc02; border-bottom: 4px solid #46a302;">
                                                        <a href="%s" target="_blank" style="font-size: 18px; font-family: 'Nunito', Arial, sans-serif; color: #ffffff; text-decoration: none; border-radius: 16px; padding: 16px 32px; display: inline-block; font-weight: 800; text-transform: uppercase; letter-spacing: 1px;">Xác nhận Email</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin-top: 32px; font-size: 14px; color: #afafaf;">
                                                Nếu nút bấm không hoạt động, bạn có thể copy và dán đường dẫn sau vào trình duyệt:<br>
                                                <a href="%s" style="color: #1cb0f6; word-break: break-all; margin-top: 8px; display: inline-block;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="background-color: #f7f9fa; padding: 24px; border-top: 2px solid #e5e5e5;">
                                            <p style="font-size: 14px; color: #afafaf; margin: 0 0 8px 0; font-weight: 700;">&copy; 2026 Bản quyền thuộc về SpeakVN.</p>
                                            <p style="font-size: 14px; color: #afafaf; margin: 0;">Bạn cần hỗ trợ? <a href="%s" style="color: #1cb0f6; text-decoration: none; font-weight: 800;">Liên hệ ngay</a></p>
                                        </td>
                                    </tr>
                                </table>
                                <table border="0" cellpadding="0" cellspacing="0" width="600" style="text-align: center; margin-top: 16px;">
                                    <tr>
                                        <td align="center">
                                            <p style="font-size: 13px; color: #afafaf; margin: 0;">
                                                Nếu bạn không tạo tài khoản này, xin hãy bỏ qua email này một cách an toàn.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(userName, otpCode, verifyLink, verifyLink, verifyLink, supportLink);
    }

    /**
     * Template HTML - Khôi phục mật khẩu (theo template Duolingo-style)
     */
    private String buildResetPasswordHtml(String userName, String resetCode, String resetLink, String supportLink) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Khôi phục mật khẩu</title>
                </head>
                <body style="font-family: 'Nunito', 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f7f9fa; margin: 0; padding: 0; color: #4b4b4b; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100%% !important;">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f9fa; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 20px; overflow: hidden; border: 2px solid #e5e5e5; text-align: center;">
                                    <tr>
                                        <td align="center" style="background-color: #ffc800; padding: 32px 20px;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: 0.5px; font-family: 'Nunito', Arial, sans-serif; text-shadow: 0 2px 4px rgba(0,0,0,0.1);">Yêu cầu khôi phục mật khẩu</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 40px 32px;">
                                            <h2 style="font-size: 22px; color: #4b4b4b; margin: 0 0 16px 0; font-weight: 800;">Thiết lập lại mật khẩu</h2>
                                            <p style="font-size: 16px; line-height: 1.6; color: #777777; margin: 0 0 24px 0;">
                                                Xin chào <strong>%s</strong>,<br><br>
                                                Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn. Đừng lo lắng, hãy sử dụng mã OTP bên dưới hoặc nhấn vào nút để tạo mật khẩu mới.
                                            </p>
                                            <!-- OTP Code Display -->
                                            <div style="margin: 24px 0;">
                                                <span style="font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #ffc800; background: #fffbeb; padding: 16px 32px; border-radius: 12px; border: 2px dashed #ffc800; display: inline-block;">
                                                    %s
                                                </span>
                                            </div>
                                            <p style="font-size: 14px; color: #ef4444; font-weight: 700; margin: 0 0 28px 0;">⏰ Mã này có hiệu lực trong 15 phút.</p>
                                            <!-- Primary Button -->
                                            <table border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="border-radius: 16px; background-color: #ff4b4b; border-bottom: 4px solid #cc3c3c;">
                                                        <a href="%s" target="_blank" style="font-size: 18px; font-family: 'Nunito', Arial, sans-serif; color: #ffffff; text-decoration: none; border-radius: 16px; padding: 16px 32px; display: inline-block; font-weight: 800; text-transform: uppercase; letter-spacing: 1px;">Đổi mật khẩu</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin-top: 32px; font-size: 14px; color: #afafaf;">
                                                Nếu nút bấm không hoạt động, bạn có thể copy và dán đường dẫn sau vào trình duyệt:<br>
                                                <a href="%s" style="color: #1cb0f6; word-break: break-all; margin-top: 8px; display: inline-block;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="background-color: #f7f9fa; padding: 24px; border-top: 2px solid #e5e5e5;">
                                            <p style="font-size: 14px; color: #afafaf; margin: 0 0 8px 0; font-weight: 700;">&copy; 2026 Bản quyền thuộc về SpeakVN.</p>
                                            <p style="font-size: 14px; color: #afafaf; margin: 0;">Bạn cần hỗ trợ? <a href="%s" style="color: #1cb0f6; text-decoration: none; font-weight: 800;">Liên hệ ngay</a></p>
                                        </td>
                                    </tr>
                                </table>
                                <table border="0" cellpadding="0" cellspacing="0" width="600" style="text-align: center; margin-top: 16px;">
                                    <tr>
                                        <td align="center">
                                            <p style="font-size: 13px; color: #afafaf; margin: 0;">
                                                Nếu bạn không yêu cầu thay đổi mật khẩu, vui lòng bỏ qua email này. Tài khoản của bạn vẫn an toàn.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(userName, resetCode, resetLink, resetLink, resetLink, supportLink);
    }

    /**
     * Template HTML - Thông báo tạo tài khoản Educator (theo template
     * Duolingo-style)
     */
    private String buildEducatorAccountHtml(String userName, String password, String loginLink, String supportLink) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Thông tin tài khoản Giáo viên</title>
                </head>
                <body style="font-family: 'Nunito', 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f7f9fa; margin: 0; padding: 0; color: #4b4b4b; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100%% !important;">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="background-color: #f7f9fa; padding: 40px 0;">
                        <tr>
                            <td align="center">
                                <table border="0" cellpadding="0" cellspacing="0" width="600" style="background-color: #ffffff; border-radius: 20px; overflow: hidden; border: 2px solid #e5e5e5; text-align: center;">
                                    <tr>
                                        <td align="center" style="background-color: #ce82ff; padding: 32px 20px;">
                                            <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 800; letter-spacing: 0.5px; font-family: 'Nunito', Arial, sans-serif; text-shadow: 0 2px 4px rgba(0,0,0,0.1);">Chào mừng Giáo viên mới!</h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding: 40px 32px;">
                                            <h2 style="font-size: 22px; color: #4b4b4b; margin: 0 0 16px 0; font-weight: 800;">Tài khoản của bạn đã sẵn sàng</h2>
                                            <p style="font-size: 16px; line-height: 1.6; color: #777777; margin: 0 0 24px 0;">
                                                Xin chào <strong>%s</strong>,<br><br>
                                                Quản trị viên vừa tạo cho bạn một tài khoản Giáo viên trên hệ thống SpeakVN. Dưới đây là mật khẩu đăng nhập tạm thời của bạn. Vui lòng đăng nhập và thay đổi mật khẩu sau khi truy cập.
                                            </p>
                                            <!-- Password Display -->
                                            <div style="margin: 24px 0;">
                                                <span style="font-size: 28px; font-weight: 800; letter-spacing: 2px; color: #ce82ff; background: #faf5ff; padding: 16px 32px; border-radius: 12px; border: 2px dashed #ce82ff; display: inline-block;">
                                                    %s
                                                </span>
                                            </div>
                                            <!-- Primary Button -->
                                            <table border="0" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="border-radius: 16px; background-color: #58cc02; border-bottom: 4px solid #46a302;">
                                                        <a href="%s" target="_blank" style="font-size: 18px; font-family: 'Nunito', Arial, sans-serif; color: #ffffff; text-decoration: none; border-radius: 16px; padding: 16px 32px; display: inline-block; font-weight: 800; text-transform: uppercase; letter-spacing: 1px;">Đăng nhập ngay</a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin-top: 32px; font-size: 14px; color: #afafaf;">
                                                Nếu nút bấm không hoạt động, bạn có thể copy và dán đường dẫn sau vào trình duyệt:<br>
                                                <a href="%s" style="color: #1cb0f6; word-break: break-all; margin-top: 8px; display: inline-block;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="background-color: #f7f9fa; padding: 24px; border-top: 2px solid #e5e5e5;">
                                            <p style="font-size: 14px; color: #afafaf; margin: 0 0 8px 0; font-weight: 700;">&copy; 2026 Bản quyền thuộc về SpeakVN.</p>
                                            <p style="font-size: 14px; color: #afafaf; margin: 0;">Bạn cần hỗ trợ? <a href="%s" style="color: #1cb0f6; text-decoration: none; font-weight: 800;">Liên hệ ngay</a></p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(userName, password, loginLink, loginLink, loginLink, supportLink);
    }
}
