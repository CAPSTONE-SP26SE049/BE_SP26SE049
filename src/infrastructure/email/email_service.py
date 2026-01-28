from fastapi_mail import FastMail, MessageSchema, ConnectionConfig, MessageType
from pydantic import EmailStr
from src.infrastructure.config.settings import settings
import logging

class EmailService:
    def __init__(self):
        self.conf = ConnectionConfig(
            MAIL_USERNAME=settings.MAIL_USERNAME,
            MAIL_PASSWORD=settings.MAIL_PASSWORD,
            MAIL_FROM=settings.MAIL_FROM,
            MAIL_PORT=settings.MAIL_PORT,
            MAIL_SERVER=settings.MAIL_SERVER,
            MAIL_STARTTLS=settings.MAIL_STARTTLS,
            MAIL_SSL_TLS=settings.MAIL_SSL_TLS,
            USE_CREDENTIALS=settings.USE_CREDENTIALS,
            VALIDATE_CERTS=settings.VALIDATE_CERTS
        )
        self.fastmail = FastMail(self.conf)

    async def send_reset_password_email(self, email_to: EmailStr, otp: str):
        """
        Send reset password email with OTP
        """
        html = f"""
        <html>
            <body>
                <h3>SpeakVN Journey - Password Reset</h3>
                <p>You requested a password reset. Use this code to reset your password:</p>
                <div style="background: #f4f4f4; padding: 10px; border-radius: 5px; text-align: center;">
                    <h2 style="letter-spacing: 5px; color: #333;">{otp}</h2>
                </div>
                <p>This code is valid for 15 minutes.</p>
                <p>If you did not request this, please ignore this email.</p>
            </body>
        </html>
        """
        
        message = MessageSchema(
            subject="Password Reset Request",
            recipients=[email_to],
            body=html,
            subtype=MessageType.html
        )
        
        try:
            await self.fastmail.send_message(message)
            logging.info(f"Email sent to {email_to}")
        except Exception as e:
            logging.error(f"Failed to send email: {e}")
            raise e
