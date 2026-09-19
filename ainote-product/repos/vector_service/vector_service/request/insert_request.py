from typing import Optional, List

from pydantic import BaseModel


class NoteInformation(BaseModel):
    note_content: str
    note_id: str
    note_theme: Optional[str]


class VectorInsertRequest(BaseModel):
    user_id: str
    notes: List[NoteInformation]
