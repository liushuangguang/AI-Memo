from pydantic import BaseModel


class SimilarNoteRequest(BaseModel):
    user_id: str
    note:str
    note_theme_id: str
