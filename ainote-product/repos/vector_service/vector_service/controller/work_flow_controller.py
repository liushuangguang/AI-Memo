from fastapi import APIRouter

from vector_service.request.same_theme_notes_request import SameThemeNotesRequest
from vector_service.responses.same_theme_note_response import SameThemeNoteResponse
from vector_service.services.vector_service import VectorService

work_flow_controller = APIRouter()


@work_flow_controller.post('/run/{workflow_id}')
def run_workflow(request: SameThemeNotesRequest, workflow_id: str):
    vector_service = VectorService()
    result = vector_service.run_work_flow(request.user_id, request.theme_name, request.theme_content, workflow_id)
    return SameThemeNoteResponse(result)
