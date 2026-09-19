from fastapi import APIRouter

from vector_service.request.insert_theme_request import InsertThemeRequest
from vector_service.responses.theme_upsert_response import ThemeUpsert, ThemeUpsertResponse
from vector_service.services.vector_service import VectorService

theme_controller = APIRouter()


@theme_controller.post(path='/upsert')
def insert_theme(insert_theme_request: InsertThemeRequest):
    vector_service = VectorService()
    try:
        related_note_ids = vector_service.upsert_theme_vector(user_id=insert_theme_request.user_id,
                                                              theme_id=insert_theme_request.theme_id,
                                                              theme_content=insert_theme_request.theme)
        theme_upsert = ThemeUpsert(user_id=insert_theme_request.user_id, theme_id=insert_theme_request.theme_id,
                                   related_note_ids=related_note_ids)
        return ThemeUpsertResponse(content=theme_upsert)
    except Exception as e:
        return ThemeUpsertResponse(content=None, message=str(e), status_code=500)
