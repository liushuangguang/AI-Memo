import json

from fastapi import Response


class SimilarNoteResponse(Response):
    media_type = "application/json"

    def __init__(self, content: dict, message: str = "Success", status_code: int = 200):
        payload = {
            "status": status_code,
            "message": message,
            "data": content,
        }
        super().__init__(content=json.dumps(payload), status_code=status_code)
