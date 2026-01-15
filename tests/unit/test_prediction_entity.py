import pytest
from src.domain.entities.model import PredictionEntity


def test_prediction_entity_creation():
    """Test creating a prediction entity"""
    prediction = PredictionEntity(
        input_data={"feature1": 0.5},
        prediction_result={"class": "A"},
        model_version="v1.0",
        confidence_score=0.95
    )

    assert prediction.input_data == {"feature1": 0.5}
    assert prediction.prediction_result == {"class": "A"}
    assert prediction.confidence_score == 0.95


def test_prediction_entity_validation():
    """Test prediction entity validation"""
    with pytest.raises(ValueError):
        PredictionEntity(input_data=None)

    with pytest.raises(ValueError):
        PredictionEntity(
            input_data={"feature1": 0.5},
            confidence_score=1.5  # Invalid: > 1
        )

