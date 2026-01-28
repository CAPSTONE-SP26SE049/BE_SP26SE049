import pytest
import os
import sys

# Ensure root path is in sys.path
sys.path.append(os.getcwd())

from src.infrastructure.email.email_service import EmailService

@pytest.mark.asyncio
async def test_send_reset_email_connection():
    """
    Test that EmailService attempts to connect to the SMTP server.
    Note: This test expects AuthenticationError if credentials are defaults.
    """
    service = EmailService()
    
    # We catch the exception because we know creds might be invalid in dev/test env
    try:
        await service.send_reset_password_email("nguyenductuan122004@gmail.com", "dummy_token")
    except Exception as e:
        error_msg = str(e)
        # Check if it failed due to Auth (which means connection worked)
        is_auth_error = "AuthenticationError" in error_msg or "Username and Password not accepted" in error_msg
        
        if is_auth_error:
            assert True, "Connection established, failed at Auth as expected."
        else:
            # If it failed for another reason (e.g. ConnectionRefused), that's a real bug
            pytest.fail(f"Failed to connect to SMTP server: {e}")
