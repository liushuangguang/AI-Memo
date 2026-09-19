import logging
from typing import List

from fastapi import APIRouter

from vector_service.request.insert_request import VectorInsertRequest
from vector_service.request.similar_note_request import SimilarNoteRequest
from vector_service.responses.note_insert_response import NoteInsertResponse, NoteInsert
from vector_service.responses.similar_note_response import SimilarNoteResponse
from vector_service.services.vector_service import VectorService

vector_controller = APIRouter()
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@vector_controller.post(path='/insert')
def insert_vector(insert_request: VectorInsertRequest):
    vector_service = VectorService()
    result = vector_service.insert_vector(user_id=insert_request.user_id, notes=insert_request.notes)
    logger.info(msg=f'Vector inserted for user {insert_request.user_id}, notes: {insert_request.notes}')
    return NoteInsertResponse(content=NoteInsert(user_id=insert_request.user_id, result=result))


@vector_controller.post(path='/similar-notes')
def find_similar_notes(similar_note_request: SimilarNoteRequest):
    vector_service = VectorService()
    similar_notes: List = vector_service.find_similar_notes(user_id=similar_note_request.user_id,
                                                            note=similar_note_request.note,
                                                            theme_id=similar_note_request.note_theme_id, threshold=0.6)
    response_content = {
        "user_id": similar_note_request.user_id,
        "related_notes_ids": similar_notes
    }
    return SimilarNoteResponse(response_content)
