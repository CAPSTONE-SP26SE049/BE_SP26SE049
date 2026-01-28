from src.infrastructure.config.settings import settings

def test_health_check(client):
    """Test health check endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == settings.APP_NAME

def test_docs_endpoint(client):
    """Test documentation endpoint exists"""
    response = client.get("/docs")
    assert response.status_code == 200
