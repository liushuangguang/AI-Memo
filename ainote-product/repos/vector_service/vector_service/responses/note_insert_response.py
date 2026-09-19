import json
from typing import List

from fastapi import Response
from pydantic import BaseModel


class RelatedItem(BaseModel):
    related_theme_id: str
    related_notes_ids: List[str]


class NoteInsertResult(BaseModel):
    note_id: str
    related_item: List[RelatedItem]


class NoteInsert(BaseModel):
    user_id: str
    result: List[NoteInsertResult]


class NoteInsertResponse(Response):
    media_type = "application/json"

    def __init__(self, content: NoteInsert, message: str = "Success", status_code: int = 200):
        payload = {
            "status": status_code,
            "message": message,
            "data": content.dict(),
        }
        super().__init__(content=json.dumps(payload), status_code=status_code)
