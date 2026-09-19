import json
from typing import Optional, List
from pydantic import BaseModel
from fastapi import Response


class ThemeUpsert(BaseModel):
    user_id: str
    theme_id: str
    related_note_ids: List[str]


class ThemeUpsertResponse(Response):
    media_type = "application/json"

    def __init__(self, content: Optional[ThemeUpsert], message: str = "Success", status_code: int = 200):
        payload = {
            "status": status_code,
            "message": message,
            "data": content.dict() if content else None,
        }
        super().__init__(content=json.dumps(payload), status_code=status_code)
